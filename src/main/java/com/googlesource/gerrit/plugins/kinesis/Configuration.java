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
  private static final String DEFAULT_NUMBER_OF_SUBSCRIBERS = "6";
  private static final String DEFAULT_STREAM_EVENTS_TOPIC = "gerrit";
  private static final String DEFAULT_INITIAL_POSITION = "latest";
  private static final Long DEFAULT_POLLING_INTERVAL_MS = 1000L;
  private static final Integer DEFAULT_MAX_RECORDS = 100;
  private static final Long DEFAULT_PUBLISH_SINGLE_REQUEST_TIMEOUT_MS = 6000L;
  private static final Long DEFAULT_PUBLISH_TIMEOUT_MS = 6000L;
  private static final Long DEFAULT_SHUTDOWN_TIMEOUT_MS = 20000L;
  private static final Level DEFAULT_AWS_LIB_LOG_LEVEL = Level.WARN;
  private static final Boolean DEFAULT_SEND_ASYNC = true;

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
  private final Level awsLibLogLevel;
  private final Boolean sendAsync;

  @Inject
  public Configuration(PluginConfigFactory configFactory, @PluginName String pluginName) {
    PluginConfig pluginConfig = configFactory.getFromGerritConfig(pluginName);

    this.region = Optional.ofNullable(getStringParam(pluginConfig, "region", null)).map(Region::of);
    this.endpoint =
        Optional.ofNullable(getStringParam(pluginConfig, "endpoint", null)).map(URI::create);
    this.streamEventsTopic = getStringParam(pluginConfig, "topic", DEFAULT_STREAM_EVENTS_TOPIC);
    this.numberOfSubscribers =
        Integer.parseInt(
            getStringParam(pluginConfig, "numberOfSubscribers", DEFAULT_NUMBER_OF_SUBSCRIBERS));
    this.applicationName = getStringParam(pluginConfig, "applicationName", pluginName);

    this.initialPosition =
        InitialPositionInStream.valueOf(
            getStringParam(pluginConfig, "initialPosition", DEFAULT_INITIAL_POSITION)
                .toUpperCase());

    this.pollingIntervalMs =
        Optional.ofNullable(getStringParam(pluginConfig, "pollingIntervalMs", null))
            .map(Long::parseLong)
            .orElse(DEFAULT_POLLING_INTERVAL_MS);

    this.maxRecords =
        Optional.ofNullable(getStringParam(pluginConfig, "maxRecords", null))
            .map(Integer::parseInt)
            .orElse(DEFAULT_MAX_RECORDS);

    this.publishSingleRequestTimeoutMs =
        Optional.ofNullable(getStringParam(pluginConfig, "publishSingleRequestTimeoutMs", null))
            .map(Long::parseLong)
            .orElse(DEFAULT_PUBLISH_SINGLE_REQUEST_TIMEOUT_MS);

    this.publishTimeoutMs =
        Optional.ofNullable(getStringParam(pluginConfig, "publishTimeoutMs", null))
            .map(Long::parseLong)
            .orElse(DEFAULT_PUBLISH_TIMEOUT_MS);

    this.shutdownTimeoutMs =
        Optional.ofNullable(getStringParam(pluginConfig, "shutdownTimeoutMs", null))
            .map(Long::parseLong)
            .orElse(DEFAULT_SHUTDOWN_TIMEOUT_MS);

    this.awsLibLogLevel =
        Optional.ofNullable(getStringParam(pluginConfig, "awsLibLogLevel", null))
            .map(l -> Level.toLevel(l, DEFAULT_AWS_LIB_LOG_LEVEL))
            .orElse(DEFAULT_AWS_LIB_LOG_LEVEL);

    this.sendAsync =
        Optional.ofNullable(getStringParam(pluginConfig, "sendAsync", null))
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

  public Level getAwsLibLogLevel() {
    return awsLibLogLevel;
  }

  public Boolean isSendAsync() {
    return sendAsync;
  }
}
