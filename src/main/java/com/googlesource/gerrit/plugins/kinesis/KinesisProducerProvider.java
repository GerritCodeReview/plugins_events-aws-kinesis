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

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;

@Singleton
public class KinesisProducerProvider implements Provider<KinesisProducer> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final Configuration configuration;
  private final AwsRegionProviderChain regionProvider;

  @Inject
  KinesisProducerProvider(Configuration configuration, AwsRegionProviderChain regionProvider) {
    this.configuration = configuration;
    this.regionProvider = regionProvider;
  }

  @Override
  public KinesisProducer get() {
    KinesisProducerConfiguration conf =
        new KinesisProducerConfiguration()
            .setAggregationEnabled(false)
            .setMaxConnections(1)
            .setRequestTimeout(configuration.getPublishSingleRequestTimeoutMs())
            .setRecordMaxBufferedTime(configuration.getPublishRecordMaxBufferedTimeMs());

    conf.setRegion(configuration.getRegion().orElseGet(regionProvider::getRegion).toString());

    configuration
        .getAwsConfigurationProfileName()
        .ifPresent(profile -> conf.setCredentialsProvider(new ProfileCredentialsProvider(profile)));
    configuration
        .getEndpoint()
        .ifPresent(
            uri ->
                conf.setKinesisEndpoint(uri.getHost())
                    .setKinesisPort(uri.getPort())
                    .setCloudwatchEndpoint(uri.getHost())
                    .setCloudwatchPort(uri.getPort())
                    .setVerifyCertificate(false));
    logger.atInfo().log(
        "Kinesis producer configured. Request Timeout (ms):'%s'%s%s%s",
        configuration.getPublishSingleRequestTimeoutMs(),
        String.format("|region: '%s'", conf.getRegion()),
        configuration
            .getEndpoint()
            .map(e -> String.format("|endpoint: '%s'", e.toASCIIString()))
            .orElse(""),
        configuration
            .getAwsConfigurationProfileName()
            .map(p -> String.format("|profile: '%s'", p))
            .orElse(""));

    return new KinesisProducer(conf);
  }
}
