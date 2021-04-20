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
import static org.mockito.Mockito.when;

import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import org.apache.log4j.Level;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationTest {
  private static final String PLUGIN_NAME = "kinesis-events";

  @Mock private PluginConfigFactory pluginConfigFactoryMock;
  private PluginConfig.Update pluginConfig;

  @Before
  public void setup() {
    pluginConfig = PluginConfig.Update.forTest(PLUGIN_NAME, new Config());
  }

  @Test
  public void shouldReadDefaultAWSLibLogLevel() {
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    Configuration configuration = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME);

    assertThat(configuration.getAwsLibLogLevel()).isEqualTo(Level.WARN);
  }

  @Test
  public void shouldConfigureSpecificAWSLibLogLevel() {
    pluginConfig.setString("awsLibLogLevel", "debug");
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    Configuration configuration = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME);

    assertThat(configuration.getAwsLibLogLevel()).isEqualTo(Level.DEBUG);
  }

  @Test
  public void shouldFallBackToDefaultWhenMisConfigured() {
    pluginConfig.setString("awsLibLogLevel", "foobar");
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    Configuration configuration = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME);

    assertThat(configuration.getAwsLibLogLevel()).isEqualTo(Level.WARN);
  }

  @Test
  public void shouldDefaultToAsynchronousPublishing() {
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    Configuration configuration = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME);

    assertThat(configuration.isSendAsync()).isEqualTo(true);
  }

  @Test
  public void shouldConfigureSynchronousPublishing() {
    pluginConfig.setBoolean("sendAsync", false);
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    Configuration configuration = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME);

    assertThat(configuration.isSendAsync()).isEqualTo(false);
  }
}
