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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import software.amazon.kinesis.coordinator.Scheduler;

class KinesisConsumer {
  interface Factory {
    KinesisConsumer create(
        @Assisted("streamName") String streamName,
        @Assisted("groupId") String groupId,
        Consumer<Event> messageProcessor);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final SchedulerProvider.Factory schedulerFactory;
  private final CheckpointResetter checkpointResetter;
  private final Configuration configuration;
  private final ExecutorService executor;

  private final String groupId;
  private Scheduler kinesisScheduler;

  private final java.util.function.Consumer<Event> messageProcessor;
  private final String streamName;
  private AtomicBoolean resetOffset = new AtomicBoolean(false);

  @Inject
  public KinesisConsumer(
      SchedulerProvider.Factory schedulerFactory,
      CheckpointResetter checkpointResetter,
      Configuration configuration,
      @ConsumerExecutor ExecutorService executor,
      @Assisted("streamName") String streamName,
      @Assisted("groupId") String groupId,
      @Assisted java.util.function.Consumer<Event> messageProcessor) {
    this.schedulerFactory = schedulerFactory;
    this.checkpointResetter = checkpointResetter;
    this.configuration = configuration;
    this.executor = executor;
    this.groupId = groupId;
    this.streamName = streamName;
    this.messageProcessor = messageProcessor;
  }

  public void subscribe() {
    logger.atInfo().log("Subscribe kinesis consumer to stream [%s]", streamName);
    runReceiver(groupId, messageProcessor);
  }

  private void runReceiver(String groupId, java.util.function.Consumer<Event> messageProcessor) {
    this.kinesisScheduler =
        schedulerFactory
            .create(streamName, groupId, resetOffset.getAndSet(false), messageProcessor)
            .get();
    executor.execute(kinesisScheduler);
  }

  public void shutdown() {
    Future<Boolean> gracefulShutdownFuture = kinesisScheduler.startGracefulShutdown();
    logger.atInfo().log(
        "Waiting up to '%s' milliseconds to complete shutdown of kinesis consumer of stream '%s'",
        configuration.getShutdownTimeoutMs(), getStreamName());
    try {
      gracefulShutdownFuture.get(configuration.getShutdownTimeoutMs(), TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      logger.atSevere().withCause(e).log(
          "Error caught when shutting down kinesis consumer for stream %s", getStreamName());
    }
    logger.atInfo().log("Shutdown kinesis consumer of stream %s completed.", getStreamName());
  }

  public java.util.function.Consumer<Event> getMessageProcessor() {
    return messageProcessor;
  }

  public String getStreamName() {
    return streamName;
  }

  public String getGroupId() {
    return groupId;
  }

  public void resetOffset() {
    // Move all checkpoints (if any) to TRIM_HORIZON, so that the consumer
    // scheduler will start consuming from beginning.
    checkpointResetter.setAllShardsToBeginning(streamName, groupId);

    // Even when no checkpoints have been persisted, instruct the consumer
    // scheduler to start from TRIM_HORIZON, irrespective of 'initialPosition'
    // configuration.
    resetOffset.set(true);
  }
}
