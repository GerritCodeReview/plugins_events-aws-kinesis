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

import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.function.Consumer;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.ShardRecordProcessor;

class KinesisRecordProcessor implements ShardRecordProcessor {
  interface Factory {
    KinesisRecordProcessor create(Consumer<EventMessage> recordProcessor);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final Consumer<EventMessage> recordProcessor;
  private final OneOffRequestContext oneOffCtx;
  private final Gson gson;

  @Inject
  KinesisRecordProcessor(
      @Assisted Consumer<EventMessage> recordProcessor, OneOffRequestContext oneOffCtx, Gson gson) {
    this.recordProcessor = recordProcessor;
    this.oneOffCtx = oneOffCtx;
    this.gson = gson;
  }

  @Override
  public void initialize(InitializationInput initializationInput) {
    logger.atInfo().log(
        "Initializing @ Sequence: %s", initializationInput.extendedSequenceNumber());
  }

  @Override
  public void processRecords(ProcessRecordsInput processRecordsInput) {
    try {
      logger.atInfo().log("Processing %s record(s)", processRecordsInput.records().size());
      processRecordsInput
          .records()
          .forEach(
              consumerRecord -> {
                logger.atFiner().log(
                    "GERRIT > Processing record pk: %s -- %s",
                    consumerRecord.partitionKey(), consumerRecord.sequenceNumber());
                byte[] byteRecord = new byte[consumerRecord.data().remaining()];
                consumerRecord.data().get(byteRecord);
                String jsonMessage = new String(byteRecord);
                logger.atFiner().log("Kinesis consumed event: '%s'", jsonMessage);
                try (ManualRequestContext ctx = oneOffCtx.open()) {
                  EventMessage eventMessage = gson.fromJson(jsonMessage, EventMessage.class);
                  recordProcessor.accept(eventMessage);
                } catch (Exception e) {
                  logger.atSevere().withCause(e).log("Could not process event '%s'", jsonMessage);
                }
              });
    } catch (Throwable t) {
      logger.atSevere().withCause(t).log("Caught throwable while processing records. Aborting.");
    }
  }

  @Override
  public void leaseLost(LeaseLostInput leaseLostInput) {
    logger.atInfo().log("Lost lease, so terminating.");
  }

  @Override
  public void shardEnded(ShardEndedInput shardEndedInput) {
    try {
      logger.atInfo().log("Reached shard end checkpointing.");
      shardEndedInput.checkpointer().checkpoint();
    } catch (ShutdownException | InvalidStateException e) {
      logger.atSevere().withCause(e).log("Exception while checkpointing at shard end. Giving up.");
    }
  }

  @Override
  public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
    try {
      logger.atInfo().log("Scheduler is shutting down, checkpointing.");
      shutdownRequestedInput.checkpointer().checkpoint();
    } catch (ShutdownException | InvalidStateException e) {
      logger.atSevere().withCause(e).log(
          "Exception while checkpointing at requested shutdown. Giving up.");
    }
  }
}
