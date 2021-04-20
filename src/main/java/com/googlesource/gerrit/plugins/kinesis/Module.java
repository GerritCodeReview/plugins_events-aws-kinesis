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

import static com.google.inject.Scopes.SINGLETON;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.EventGsonProvider;
import com.gerritforge.gerrit.eventbroker.TopicSubscriber;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.events.EventListener;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

public class Module extends LifecycleModule {
  private Set<TopicSubscriber> activeConsumers = Sets.newHashSet();

  /**
   * By default the events-broker library (loaded directly by the multi-site) registers a noop
   * implementation, which itself registers a list of topic subscribers. Since we have no control
   * over which plugin is loaded first (multi-site or kinesis-events), we deal with the possibility
   * that a broker api was already registered and, in that case, reassign its consumers to the
   * kinesis-events plugin. The injection is optional because if the kinesis-events plugin is loaded
   * first, then there will be no previously registered broker API.
   *
   * @param previousBrokerApi
   */
  @Inject(optional = true)
  public void setPreviousBrokerApi(DynamicItem<BrokerApi> previousBrokerApi) {
    if (previousBrokerApi != null && previousBrokerApi.get() != null) {
      this.activeConsumers = previousBrokerApi.get().topicSubscribers();
    }
  }

  @Override
  protected void configure() {
    factory(KinesisRecordProcessor.Factory.class);
    factory(KinesisRecordProcessorFactory.Factory.class);
    bind(ExecutorService.class)
        .annotatedWith(ConsumerExecutor.class)
        .toProvider(ConsumerExecutorProvider.class)
        .in(SINGLETON);
    bind(ExecutorService.class)
        .annotatedWith(ProducerCallbackExecutor.class)
        .toProvider(ProducerCallbackExecutorProvider.class)
        .in(SINGLETON);
    bind(KinesisProducer.class).toProvider(KinesisProducerProvider.class).in(Scopes.SINGLETON);
    bind(KinesisAsyncClient.class).toProvider(KinesisAsyncClientProvider.class).in(SINGLETON);
    bind(DynamoDbAsyncClient.class).toProvider(DynamoDbAsyncClientProvider.class).in(SINGLETON);
    bind(CloudWatchAsyncClient.class).toProvider(CloudWatchAsyncClientProvider.class).in(SINGLETON);
    factory(SchedulerProvider.Factory.class);
    bind(new TypeLiteral<Set<TopicSubscriber>>() {}).toInstance(activeConsumers);
    DynamicItem.bind(binder(), BrokerApi.class).to(KinesisBrokerApi.class).in(Scopes.SINGLETON);
    bind(Gson.class).toProvider(EventGsonProvider.class).in(Singleton.class);
    DynamicSet.bind(binder(), LifecycleListener.class).to(KinesisBrokerLifeCycleManager.class);
    factory(KinesisConsumer.Factory.class);
    DynamicSet.bind(binder(), EventListener.class).to(KinesisPublisher.class);
    listener().to(AWSLogLevelListener.class);
  }
}
