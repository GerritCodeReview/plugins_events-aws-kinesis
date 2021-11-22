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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;

@RunWith(MockitoJUnitRunner.class)
public class KinesisProducerProviderTest {
  private KinesisProducerProvider objectUnderTest;

  @Mock Configuration configuration;
  @Mock AwsRegionProviderChain regionProvider;

  @Before
  public void setup() {
    long aRequestTimeout = 1000L;
    when(configuration.getPublishSingleRequestTimeoutMs()).thenReturn(aRequestTimeout);
    objectUnderTest = new KinesisProducerProvider(configuration, regionProvider);
  }

  @Test
  public void shouldCallRegionProviderWhenRegionNotExplicitlyConfigured() {
    when(configuration.getRegion()).thenReturn(Optional.empty());
    when(regionProvider.getRegion()).thenReturn(Region.US_EAST_1);

    KinesisProducer kinesisProducer = objectUnderTest.get();

    verify(regionProvider).getRegion();
  }

  @Test
  public void shouldNotCallRegionProviderWhenRegionIsExplicitlyConfigured() {
    when(configuration.getRegion()).thenReturn(Optional.of(Region.US_EAST_1));

    KinesisProducer kinesisProducer = objectUnderTest.get();

    verify(regionProvider, never()).getRegion();
  }
}
