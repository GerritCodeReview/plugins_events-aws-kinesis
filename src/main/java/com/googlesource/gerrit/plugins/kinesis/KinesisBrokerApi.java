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

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.TopicSubscriber;
import com.gerritforge.gerrit.eventbroker.TopicSubscriberWithGroupId;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class KinesisBrokerApi implements BrokerApi {
  private final KinesisConsumer.Factory consumerFactory;
  private final Configuration configuration;

  private final KinesisPublisher kinesisPublisher;
  private final Set<KinesisConsumer> consumers;

  @Inject
  public KinesisBrokerApi(
      KinesisPublisher kinesisPublisher,
      KinesisConsumer.Factory consumerFactory,
      Configuration configuration) {
    this.kinesisPublisher = kinesisPublisher;
    this.consumerFactory = consumerFactory;
    this.configuration = configuration;
    this.consumers = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  @Override
  public ListenableFuture<Boolean> send(String streamName, Event event) {
    return kinesisPublisher.publish(streamName, event);
  }

  @Override
  public void receiveAsync(String streamName, Consumer<Event> eventConsumer) {
    receive(streamName, eventConsumer, null);
  }

  @Override
  public void receiveAsync(String streamName, String groupId, Consumer<Event> consumer) {
    receive(streamName, consumer, groupId);
  }

  @Override
  public Set<TopicSubscriber> topicSubscribers() {
    return consumers.stream()
        .map(s -> TopicSubscriber.topicSubscriber(s.getStreamName(), s.getMessageProcessor()))
        .collect(Collectors.toSet());
  }

  @Override
  public void disconnect() {
    consumers.parallelStream().forEach(KinesisConsumer::shutdown);
    consumers.clear();
  }

  @Override
  public void disconnect(String topic, String groupId) {
    Set<KinesisConsumer> consumersOfTopic =
        consumers
            .parallelStream()
            .filter(c -> topic.equals(c.getStreamName()) && groupId.equals(c.getGroupId()))
            .collect(Collectors.toSet());

    consumersOfTopic.forEach(KinesisConsumer::shutdown);
    consumers.removeAll(consumersOfTopic);
  }

  @Override
  public void replayAllEvents(String topic) {
    consumers.stream()
        .filter(subscriber -> topic.equals(subscriber.getStreamName()))
        .forEach(KinesisConsumer::resetOffset);
  }

  @Override
  public Set<TopicSubscriberWithGroupId> topicSubscribersWithGroupId() {
    return consumers.stream()
        .map(
            s ->
                TopicSubscriberWithGroupId.topicSubscriberWithGroupId(
                    s.getGroupId(),
                    TopicSubscriber.topicSubscriber(s.getStreamName(), s.getMessageProcessor())))
        .collect(Collectors.toSet());
  }

  private void receive(
      String streamName, Consumer<Event> eventConsumer, @Nullable String maybeGroupId) {
    String groupId = Optional.ofNullable(maybeGroupId).orElse(configuration.getApplicationName());
    KinesisConsumer consumer = consumerFactory.create(streamName, groupId, eventConsumer);
    consumers.add(consumer);
    consumer.subscribe(streamName, eventConsumer);
  }
}
