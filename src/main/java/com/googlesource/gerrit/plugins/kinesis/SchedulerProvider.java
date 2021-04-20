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

import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.retrieval.RetrievalConfig;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

class SchedulerProvider implements Provider<Scheduler> {
  interface Factory {
    SchedulerProvider create(
        String streamName,
        boolean fromBeginning,
        java.util.function.Consumer<EventMessage> messageProcessor);
  }

  private final ConfigsBuilder configsBuilder;
  private final Configuration configuration;
  private final KinesisAsyncClient kinesisAsyncClient;
  private final String streamName;
  private final boolean fromBeginning;

  @AssistedInject
  SchedulerProvider(
      Configuration configuration,
      KinesisAsyncClient kinesisAsyncClient,
      DynamoDbAsyncClient dynamoDbAsyncClient,
      CloudWatchAsyncClient cloudWatchAsyncClient,
      KinesisRecordProcessorFactory.Factory kinesisRecordProcessorFactory,
      @Assisted String streamName,
      @Assisted boolean fromBeginning,
      @Assisted java.util.function.Consumer<EventMessage> messageProcessor) {
    this.configuration = configuration;
    this.kinesisAsyncClient = kinesisAsyncClient;
    this.streamName = streamName;
    this.fromBeginning = fromBeginning;
    this.configsBuilder =
        new ConfigsBuilder(
            streamName,
            cosumerLeaseName(configuration.getApplicationName(), streamName),
            kinesisAsyncClient,
            dynamoDbAsyncClient,
            cloudWatchAsyncClient,
            String.format("klc-worker-%s-%s", configuration.getApplicationName(), streamName),
            kinesisRecordProcessorFactory.create(messageProcessor));
  }

  private RetrievalConfig getRetrievalConfig() {
    PollingConfig polling =
        new PollingConfig(streamName, kinesisAsyncClient)
            .idleTimeBetweenReadsInMillis(configuration.getPollingIntervalMs())
            .maxRecords(configuration.getMaxRecords());
    RetrievalConfig retrievalConfig =
        configsBuilder.retrievalConfig().retrievalSpecificConfig(polling);
    retrievalConfig.initialPositionInStreamExtended(
        InitialPositionInStreamExtended.newInitialPosition(
            fromBeginning
                ? InitialPositionInStream.TRIM_HORIZON
                : configuration.getInitialPosition()));
    return retrievalConfig;
  }

  @Override
  public Scheduler get() {
    return new Scheduler(
        configsBuilder.checkpointConfig(),
        configsBuilder.coordinatorConfig(),
        configsBuilder.leaseManagementConfig(),
        configsBuilder.lifecycleConfig(),
        configsBuilder.metricsConfig(),
        configsBuilder.processorConfig(),
        getRetrievalConfig());
  }
}
