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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.WaitUtil;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;

@TestPlugin(
    name = "events-aws-kinesis",
    sysModule = "com.googlesource.gerrit.plugins.kinesis.Module")
public class KinesisEventsIT extends LightweightPluginDaemonTest {
  // This timeout is quite high to allow the kinesis coordinator to acquire a
  // lease on the newly created stream
  private static final Duration WAIT_FOR_CONSUMPTION = Duration.ofSeconds(120);
  private static final Duration STREAM_CREATION_TIMEOUT = Duration.ofSeconds(10);
  private static final long SEND_TIMEOUT_MILLIS = 200;
  private static final String STREAM_EVENTS = "stream_events";

  private static final int LOCALSTACK_PORT = 4566;

  private LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.17.5"))
          .withServices(DYNAMODB, KINESIS, CLOUDWATCH)
          .withEnv("USE_SSL", "true")
          .withExposedPorts(LOCALSTACK_PORT);

  private KinesisClient kinesisClient;

  @Before
  public void setUpTestPlugin() throws Exception {
    localstack.start();

    kinesisClient =
        KinesisClient.builder()
            .endpointOverride(localstack.getEndpointOverride(KINESIS))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey())))
            .region(Region.of(localstack.getRegion()))
            .build();

    System.setProperty("endpoint", localstack.getEndpointOverride(KINESIS).toASCIIString());
    System.setProperty("region", localstack.getRegion());
    System.setProperty("aws.accessKeyId", localstack.getAccessKey());

    // The secret key property name has changed from aws-sdk 1.11.x and 2.x [1]
    // Export both names so that default credential provider chains work for both
    // Kinesis Consumer Library (uses V2) and Kinesis Producer Library (uses v1)
    // [1]  https://docs.aws.amazon.com/sdk-for-java/latest/migration-guide/client-credential.html
    System.setProperty("aws.secretKey", localstack.getSecretKey());
    System.setProperty("aws.secretAccessKey", localstack.getSecretKey());

    super.setUpTestPlugin();
  }

  @Override
  public void tearDownTestPlugin() {
    localstack.close();

    super.tearDownTestPlugin();
  }

  @Test
  @GerritConfig(name = "plugin.events-aws-kinesis.applicationName", value = "test-consumer")
  @GerritConfig(name = "plugin.events-aws-kinesis.initialPosition", value = "trim_horizon")
  public void shouldConsumeAnEventPublishedToATopic() throws Exception {
    String streamName = UUID.randomUUID().toString();
    createStreamAndWait(streamName, STREAM_CREATION_TIMEOUT);

    EventConsumerCounter eventConsumerCounter = new EventConsumerCounter();
    kinesisBroker().receiveAsync(streamName, eventConsumerCounter);

    Event event = eventMessage();
    kinesisBroker().send(streamName, event);
    WaitUtil.waitUntil(
        () -> eventConsumerCounter.getConsumedMessages().size() == 1, WAIT_FOR_CONSUMPTION);
    compareEvents(eventConsumerCounter.getConsumedMessages().get(0), event);
  }

  @Test
  @GerritConfig(name = "plugin.events-aws-kinesis.applicationName", value = "test-consumer")
  @GerritConfig(name = "plugin.events-aws-kinesis.initialPosition", value = "trim_horizon")
  public void shouldConsumeEventBySubscribingWithSpecificGroupId() throws Exception {
    String streamName = UUID.randomUUID().toString();
    String groupId = UUID.randomUUID().toString();
    createStreamAndWait(streamName, STREAM_CREATION_TIMEOUT);

    EventConsumerCounter eventConsumerCounter = new EventConsumerCounter();
    kinesisBroker().receiveAsync(streamName, groupId, eventConsumerCounter);

    Event event = eventMessage();
    kinesisBroker().send(streamName, event);
    WaitUtil.waitUntil(
        () -> eventConsumerCounter.getConsumedMessages().size() == 1, WAIT_FOR_CONSUMPTION);
    compareEvents(eventConsumerCounter.getConsumedMessages().get(0), event);

    assertThat(countSubscribers(streamName, groupId)).isEqualTo(1L);
  }

  @Test
  @GerritConfig(name = "plugin.events-aws-kinesis.applicationName", value = "test-consumer")
  @GerritConfig(name = "plugin.events-aws-kinesis.initialPosition", value = "trim_horizon")
  public void shouldDisconnectOnlySubscriberForSpecificGroupId() throws Exception {
    String streamName = UUID.randomUUID().toString();
    String groupId1 = UUID.randomUUID().toString();
    String groupId2 = UUID.randomUUID().toString();
    createStreamAndWait(streamName, STREAM_CREATION_TIMEOUT);

    EventConsumerCounter eventConsumerCounter = new EventConsumerCounter();
    kinesisBroker().receiveAsync(streamName, groupId1, eventConsumerCounter);
    kinesisBroker().receiveAsync(streamName, groupId2, eventConsumerCounter);

    kinesisBroker().disconnect(streamName, groupId2);

    assertThat(countSubscribers(streamName, groupId2)).isEqualTo(0L);
    assertThat(countSubscribers(streamName, groupId1)).isEqualTo(1L);
  }

  @Test
  @GerritConfig(name = "plugin.events-aws-kinesis.applicationName", value = "test-consumer")
  @GerritConfig(name = "plugin.events-aws-kinesis.initialPosition", value = "trim_horizon")
  public void shouldConsumeAnEventWithoutInstanceId() throws Exception {
    String streamName = UUID.randomUUID().toString();
    createStreamAndWait(streamName, STREAM_CREATION_TIMEOUT);

    EventConsumerCounter eventConsumerCounter = new EventConsumerCounter();
    kinesisBroker().receiveAsync(streamName, eventConsumerCounter);

    Event event = eventMessage();
    event.instanceId = null;

    kinesisBroker().send(streamName, event);
    WaitUtil.waitUntil(
        () -> eventConsumerCounter.getConsumedMessages().size() == 1, WAIT_FOR_CONSUMPTION);
    compareEvents(eventConsumerCounter.getConsumedMessages().get(0), event);
  }

  @Test
  @GerritConfig(name = "plugin.events-aws-kinesis.applicationName", value = "test-consumer")
  @GerritConfig(name = "plugin.events-aws-kinesis.initialPosition", value = "trim_horizon")
  public void shouldReplayMessages() throws Exception {
    String streamName = UUID.randomUUID().toString();
    createStreamAndWait(streamName, STREAM_CREATION_TIMEOUT);

    EventConsumerCounter eventConsumerCounter = new EventConsumerCounter();
    kinesisBroker().receiveAsync(streamName, eventConsumerCounter);

    Event event = eventMessage();
    kinesisBroker().send(streamName, event);

    WaitUtil.waitUntil(
        () -> eventConsumerCounter.getConsumedMessages().size() == 1, WAIT_FOR_CONSUMPTION);
    compareEvents(eventConsumerCounter.getConsumedMessages().get(0), event);

    eventConsumerCounter.clear();
    kinesisBroker().disconnect();
    kinesisBroker().receiveAsync(streamName, eventConsumerCounter);
    kinesisBroker().replayAllEvents(streamName);

    WaitUtil.waitUntil(
        () -> eventConsumerCounter.getConsumedMessages().size() == 1, WAIT_FOR_CONSUMPTION);
    compareEvents(eventConsumerCounter.getConsumedMessages().get(0), event);
  }

  @Test
  @GerritConfig(name = "plugin.events-aws-kinesis.applicationName", value = "test-consumer")
  @GerritConfig(name = "plugin.events-aws-kinesis.initialPosition", value = "trim_horizon")
  @GerritConfig(name = "plugin.events-aws-kinesis.publishTimeoutMs", value = "10000")
  @GerritConfig(name = "plugin.events-aws-kinesis.sendAsync", value = "false")
  public void sendingSynchronouslyShouldBeSuccessful()
      throws InterruptedException, ExecutionException {
    String streamName = UUID.randomUUID().toString();
    createStreamAsync(streamName);

    ListenableFuture<Boolean> result = kinesisBroker().send(streamName, eventMessage());
    assertThat(result.get()).isTrue();
  }

  @Test
  @GerritConfig(name = "plugin.events-aws-kinesis.applicationName", value = "test-consumer")
  @GerritConfig(name = "plugin.events-aws-kinesis.initialPosition", value = "trim_horizon")
  @GerritConfig(name = "plugin.events-aws-kinesis.sendAsync", value = "false")
  public void sendingSynchronouslyShouldBeUnsuccessfulWhenTimingOut()
      throws InterruptedException, ExecutionException, TimeoutException {
    String streamName = "not-existing-stream";

    ListenableFuture<Boolean> result = kinesisBroker().send(streamName, eventMessage());
    assertThat(result.get(SEND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isFalse();
  }

  @Test
  @GerritConfig(name = "plugin.events-aws-kinesis.applicationName", value = "test-consumer")
  @GerritConfig(name = "plugin.events-aws-kinesis.initialPosition", value = "trim_horizon")
  @GerritConfig(name = "plugin.events-aws-kinesis.sendAsync", value = "true")
  public void sendingAsynchronouslyShouldFailWhenStreamDoesNotExist() {
    String streamName = "not-existing-stream";

    ListenableFuture<Boolean> result = kinesisBroker().send(streamName, eventMessage());
    ExecutionException executionException = assertThrows(ExecutionException.class, result::get);
    assertThat(executionException)
        .hasMessageThat()
        .contains("com.amazonaws.services.kinesis.producer.UserRecordFailedException");
  }

  @Test
  @GerritConfig(name = "plugin.events-aws-kinesis.applicationName", value = "test-consumer")
  @GerritConfig(name = "plugin.events-aws-kinesis.initialPosition", value = "trim_horizon")
  @GerritConfig(name = "plugin.events-aws-kinesis.sendAsync", value = "true")
  public void sendingAsynchronouslyShouldBeSuccessful()
      throws InterruptedException, ExecutionException {
    String streamName = UUID.randomUUID().toString();
    createStreamAsync(streamName);

    ListenableFuture<Boolean> result = kinesisBroker().send(streamName, eventMessage());
    assertThat(result.get()).isTrue();
  }

  @Test
  @GerritConfig(name = "plugin.events-aws-kinesis.applicationName", value = "test-consumer")
  @GerritConfig(name = "plugin.events-aws-kinesis.initialPosition", value = "trim_horizon")
  @GerritConfig(name = "plugin.events-aws-kinesis.topic", value = STREAM_EVENTS)
  @GerritConfig(name = "plugin.events-aws-kinesis.sendStreamEvents", value = "true")
  public void shouldSendStreamEventsWhenEnabled() throws Exception {
    createStreamAndWait(STREAM_EVENTS, STREAM_CREATION_TIMEOUT);

    EventConsumerCounter eventConsumerCounter = new EventConsumerCounter();
    kinesisBroker().receiveAsync(STREAM_EVENTS, eventConsumerCounter);

    createChange();

    // There are 4 events are received in the following order:
    // 1. refUpdate:        ref: refs/sequences/changes
    // 2. refUpdate:        ref: refs/changes/01/1/1
    // 3. refUpdate:        ref: refs/changes/01/1/meta
    // 4. patchset-created: ref: refs/changes/01/1/1
    WaitUtil.waitUntil(
        () -> eventConsumerCounter.getConsumedMessages().size() == 4, WAIT_FOR_CONSUMPTION);
  }

  public KinesisBrokerApi kinesisBroker() {
    return (KinesisBrokerApi) plugin.getSysInjector().getInstance(BrokerApi.class);
  }

  private void createStreamAndWait(String streamName, Duration timeout)
      throws InterruptedException {
    createStreamAsync(streamName);

    WaitUtil.waitUntil(
        () ->
            kinesisClient
                .describeStream(DescribeStreamRequest.builder().streamName(streamName).build())
                .streamDescription()
                .streamStatus()
                .equals(StreamStatus.ACTIVE),
        timeout);
  }

  private void createStreamAsync(String streamName) {
    kinesisClient.createStream(
        CreateStreamRequest.builder().streamName(streamName).shardCount(1).build());
  }

  private Event eventMessage() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = "instance-id";
    return event;
  }

  private void compareEvents(Event event, Event expectedEvent) {
    assertThat(event.type).isEqualTo(expectedEvent.type);
    assertThat(event.eventCreatedOn).isEqualTo(expectedEvent.eventCreatedOn);
    assertThat(event.instanceId).isEqualTo(expectedEvent.instanceId);
  }

  private long countSubscribers(String streamName, String groupId) {
    return kinesisBroker().topicSubscribersWithGroupId().stream()
        .filter(t -> groupId.equals(t.groupId()) && streamName.equals(t.topicSubscriber().topic()))
        .count();
  }

  private static class EventConsumerCounter implements Consumer<Event> {
    List<Event> consumedMessages = new ArrayList<>();

    @Override
    public void accept(Event eventMessage) {
      consumedMessages.add(eventMessage);
    }

    public List<Event> getConsumedMessages() {
      return consumedMessages;
    }

    public void clear() {
      consumedMessages.clear();
    }
  }
}
