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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.kinesis.common.KinesisClientUtil;

@Singleton
class KinesisAsyncClientProvider implements Provider<KinesisAsyncClient> {
  private final Configuration configuration;

  @Inject
  KinesisAsyncClientProvider(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public KinesisAsyncClient get() {
    KinesisAsyncClientBuilder builder = KinesisAsyncClient.builder();
    configuration.getRegion().ifPresent(builder::region);
    configuration.getEndpoint().ifPresent(builder::endpointOverride);
    configuration
        .getAwsConfigurationProfileName()
        .ifPresent(
            profile -> builder.credentialsProvider(ProfileCredentialsProvider.create(profile)));
    return KinesisClientUtil.createKinesisAsyncClient(builder);
  }
}
