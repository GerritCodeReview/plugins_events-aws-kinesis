// Copyright (C) 2021 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.kinesis;

import static com.googlesource.gerrit.plugins.kinesis.Configuration.cosumerLeaseName;
import static software.amazon.kinesis.common.InitialPositionInStream.TRIM_HORIZON;

import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

@Singleton
class CheckpointResetter {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String LEASE_KEY_ATTRIBUTE_NAME = "leaseKey";
  private static final String LEASE_CHECKPOINT_ATTRIBUTE_VAUE = "checkpoint";
  private static final Integer DYNAMODB_RESPONSE_TIMEOUT_SECS = 5;

  private final Configuration configuration;
  private final DynamoDbAsyncClient dynamoDbAsyncClient;

  @Inject
  CheckpointResetter(Configuration configuration, DynamoDbAsyncClient dynamoDbAsyncClient) {
    this.configuration = configuration;
    this.dynamoDbAsyncClient = dynamoDbAsyncClient;
  }

  public void setAllShardsToBeginning(String streamName) {
    String leaseTable = cosumerLeaseName(configuration.getApplicationName(), streamName);

    try {
      for (String shard : getAllShards(leaseTable)) {
        logger.atInfo().log("[%s - %s] Resetting checkpoint", leaseTable, shard);

        Map<String, AttributeValue> updateKey = new HashMap<>();
        updateKey.put(LEASE_KEY_ATTRIBUTE_NAME, AttributeValue.builder().s(shard).build());

        Map<String, AttributeValueUpdate> updateValues = new HashMap<>();
        updateValues.put(
            LEASE_CHECKPOINT_ATTRIBUTE_VAUE,
            AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(TRIM_HORIZON.name().toUpperCase()).build())
                .build());

        UpdateItemResponse updateItemResponse =
            dynamoDbAsyncClient
                .updateItem(
                    UpdateItemRequest.builder()
                        .tableName(leaseTable)
                        .key(updateKey)
                        .attributeUpdates(updateValues)
                        .returnValues(ReturnValue.ALL_OLD)
                        .build())
                .get(DYNAMODB_RESPONSE_TIMEOUT_SECS, TimeUnit.SECONDS);

        logger.atInfo().log(
            "[%s - %s] Successfully reset checkpoints. old value: %s",
            leaseTable, shard, updateItemResponse);
      }
    } catch (InterruptedException e) {
      logger.atWarning().log("%s resetOffset: interrupted", leaseTable);
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e).log("%s resetOffset: Error", leaseTable);
    } catch (TimeoutException e) {
      logger.atSevere().withCause(e).log("%s resetOffset: Timeout", leaseTable);
    }
  }

  private Set<String> getAllShards(String leaseTable)
      throws InterruptedException, ExecutionException, TimeoutException {
    try {
      ScanRequest scanRequest =
          ScanRequest.builder()
              .tableName(leaseTable)
              .attributesToGet(LEASE_KEY_ATTRIBUTE_NAME)
              .build();

      ScanResponse scanResponse =
          dynamoDbAsyncClient
              .scan(scanRequest)
              .get(DYNAMODB_RESPONSE_TIMEOUT_SECS, TimeUnit.SECONDS);
      return scanResponse.items().stream()
          .map(i -> i.get(LEASE_KEY_ATTRIBUTE_NAME).s())
          .collect(Collectors.toSet());
    } catch (Exception e) {
      if (e.getCause() != null && e.getCause() instanceof ResourceNotFoundException) {
        logger.atWarning().log(
            "%s resetOffset: lease table does not exist, nothing to reset.", leaseTable);
        return Collections.emptySet();
      }
      throw e;
    }
  }
}
