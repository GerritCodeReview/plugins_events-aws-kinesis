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

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.URI;
import java.util.Optional;
import org.apache.log4j.Level;
import software.amazon.awssdk.regions.Region;
import software.amazon.kinesis.common.InitialPositionInStream;

@Singleton
class Configuration {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String REGION_FIELD = "region";
  static final String ENDPOINT_FIELD = "endpoint";
  static final String STREAM_EVENTS_TOPIC_FIELD = "topic";
  static final String NUMBER_OF_SUBSCRIBERS_FIELD = "numberOfSubscribers";
  static final String APPLICATION_NAME_FIELD = "applicationName";
  static final String INITIAL_POSITION_FIELD = "initialPosition";
  static final String POLLING_INTERVAL_MS_FIELD = "pollingIntervalMs";
  static final String MAX_RECORDS_FIELD = "maxRecords";
  static final String PUBLISH_SINGLE_REQUEST_TIMEOUT_MS_FIELD = "publishSingleRequestTimeoutMs";
  static final String PUBLISH_TIMEOUT_MS_FIELD = "publishTimeoutMs";
  static final String SHUTDOWN_MS_FIELD = "shutdownTimeoutMs";
  static final String AWS_LIB_LOG_LEVEL_FIELD = "awsLibLogLevel";
  static final String SEND_ASYNC_FIELD = "sendAsync";
  static final String SEND_STREAM_EVENTS_FIELD = "sendStreamEvents";
  static final Boolean DEFAULT_SEND_STREAM_EVENTS = false;
  static final String DEFAULT_NUMBER_OF_SUBSCRIBERS = "6";
  static final String DEFAULT_STREAM_EVENTS_TOPIC = "gerrit";
  static final String DEFAULT_INITIAL_POSITION = "latest";
  static final Long DEFAULT_POLLING_INTERVAL_MS = 1000L;
  static final Integer DEFAULT_MAX_RECORDS = 100;
  static final Long DEFAULT_PUBLISH_SINGLE_REQUEST_TIMEOUT_MS = 6000L;
  static final Long DEFAULT_PUBLISH_TIMEOUT_MS = 6000L;
  static final Long DEFAULT_SHUTDOWN_TIMEOUT_MS = 20000L;
  static final Long DEFAULT_CHECKPOINT_INTERVAL_MS = 5 * 60000L; // 5 min
  static final Level DEFAULT_AWS_LIB_LOG_LEVEL = Level.WARN;
  static final Boolean DEFAULT_SEND_ASYNC = true;

  private final String applicationName;
  private final String streamEventsTopic;
  private final int numberOfSubscribers;
  private final InitialPositionInStream initialPosition;
  private final Optional<Region> region;
  private final Optional<URI> endpoint;
  private final Long pollingIntervalMs;
  private final Integer maxRecords;
  private final Long publishTimeoutMs;
  private final Long publishSingleRequestTimeoutMs;
  private final Long shutdownTimeoutMs;
  private final Long checkpointIntervalMs;
  private final Level awsLibLogLevel;
  private final Boolean sendAsync;
  private final Boolean sendStreamEvents;

  @Inject
  public Configuration(PluginConfigFactory configFactory, @PluginName String pluginName) {
    PluginConfig pluginConfig = configFactory.getFromGerritConfig(pluginName);

    this.region =
        Optional.ofNullable(getStringParam(pluginConfig, REGION_FIELD, null)).map(Region::of);
    this.endpoint =
        Optional.ofNullable(getStringParam(pluginConfig, ENDPOINT_FIELD, null)).map(URI::create);
    this.streamEventsTopic =
        getStringParam(pluginConfig, STREAM_EVENTS_TOPIC_FIELD, DEFAULT_STREAM_EVENTS_TOPIC);
    this.sendStreamEvents =
        Optional.ofNullable(getStringParam(pluginConfig, SEND_STREAM_EVENTS_FIELD, null))
            .map(Boolean::new)
            .orElse(DEFAULT_SEND_STREAM_EVENTS);
    this.numberOfSubscribers =
        Integer.parseInt(
            getStringParam(
                pluginConfig, NUMBER_OF_SUBSCRIBERS_FIELD, DEFAULT_NUMBER_OF_SUBSCRIBERS));
    this.applicationName = getStringParam(pluginConfig, APPLICATION_NAME_FIELD, pluginName);

    this.initialPosition =
        InitialPositionInStream.valueOf(
            getStringParam(pluginConfig, INITIAL_POSITION_FIELD, DEFAULT_INITIAL_POSITION)
                .toUpperCase());

    this.pollingIntervalMs =
        Optional.ofNullable(getStringParam(pluginConfig, POLLING_INTERVAL_MS_FIELD, null))
            .map(Long::parseLong)
            .orElse(DEFAULT_POLLING_INTERVAL_MS);

    this.maxRecords =
        Optional.ofNullable(getStringParam(pluginConfig, MAX_RECORDS_FIELD, null))
            .map(Integer::parseInt)
            .orElse(DEFAULT_MAX_RECORDS);

    this.publishSingleRequestTimeoutMs =
        Optional.ofNullable(
                getStringParam(pluginConfig, PUBLISH_SINGLE_REQUEST_TIMEOUT_MS_FIELD, null))
            .map(Long::parseLong)
            .orElse(DEFAULT_PUBLISH_SINGLE_REQUEST_TIMEOUT_MS);

    this.publishTimeoutMs =
        Optional.ofNullable(getStringParam(pluginConfig, PUBLISH_TIMEOUT_MS_FIELD, null))
            .map(Long::parseLong)
            .orElse(DEFAULT_PUBLISH_TIMEOUT_MS);

    this.shutdownTimeoutMs =
        Optional.ofNullable(getStringParam(pluginConfig, SHUTDOWN_MS_FIELD, null))
            .map(Long::parseLong)
            .orElse(DEFAULT_SHUTDOWN_TIMEOUT_MS);

    this.checkpointIntervalMs =
        Optional.ofNullable(getStringParam(pluginConfig, "checkpointIntervalMs", null))
            .map(Long::parseLong)
            .orElse(DEFAULT_CHECKPOINT_INTERVAL_MS);

    this.awsLibLogLevel =
        Optional.ofNullable(getStringParam(pluginConfig, AWS_LIB_LOG_LEVEL_FIELD, null))
            .map(l -> Level.toLevel(l, DEFAULT_AWS_LIB_LOG_LEVEL))
            .orElse(DEFAULT_AWS_LIB_LOG_LEVEL);

    this.sendAsync =
        Optional.ofNullable(getStringParam(pluginConfig, SEND_ASYNC_FIELD, null))
            .map(Boolean::new)
            .orElse(DEFAULT_SEND_ASYNC);

    logger.atInfo().log(
        "Kinesis client. Application:'%s'|PollingInterval: %s|maxRecords: %s%s%s",
        applicationName,
        pollingIntervalMs,
        maxRecords,
        region.map(r -> String.format("|region: %s", r.id())).orElse(""),
        endpoint.map(e -> String.format("|endpoint: %s", e.toASCIIString())).orElse(""));
  }

  public String getStreamEventsTopic() {
    return streamEventsTopic;
  }

  public int getNumberOfSubscribers() {
    return numberOfSubscribers;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public Long getPublishTimeoutMs() {
    return publishTimeoutMs;
  }

  public Optional<Region> getRegion() {
    return region;
  }

  public Optional<URI> getEndpoint() {
    return endpoint;
  }

  public Long getPublishSingleRequestTimeoutMs() {
    return publishSingleRequestTimeoutMs;
  }

  public Long getPollingIntervalMs() {
    return pollingIntervalMs;
  }

  public Integer getMaxRecords() {
    return maxRecords;
  }

  public InitialPositionInStream getInitialPosition() {
    return initialPosition;
  }

  private static String getStringParam(
      PluginConfig pluginConfig, String name, String defaultValue) {
    return Strings.isNullOrEmpty(System.getProperty(name))
        ? pluginConfig.getString(name, defaultValue)
        : System.getProperty(name);
  }

  public static String cosumerLeaseName(String applicationName, String streamName) {
    return String.format("%s-%s", applicationName, streamName);
  }

  public Long getShutdownTimeoutMs() {
    return shutdownTimeoutMs;
  }

  public Long getCheckpointIntervalMs() {
    return checkpointIntervalMs;
  }

  public Level getAwsLibLogLevel() {
    return awsLibLogLevel;
  }

  public Boolean isSendAsync() {
    return sendAsync;
  }

  public Boolean isSendStreamEvents() {
    return sendStreamEvents;
  }
}
