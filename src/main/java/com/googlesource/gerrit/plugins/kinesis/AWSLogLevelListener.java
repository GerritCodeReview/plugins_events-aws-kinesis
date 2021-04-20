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

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;
import java.util.stream.Stream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class AWSLogLevelListener implements LifecycleListener {
  private final Level awsLibLogLevel;

  @Inject
  AWSLogLevelListener(Configuration configuration) {
    this.awsLibLogLevel = configuration.getAwsLibLogLevel();
  }

  @Override
  public void start() {
    Stream.of("software.amazon", "com.amazonaws")
        .forEach(s -> Logger.getLogger(s).setLevel(awsLibLogLevel));
  }

  @Override
  public void stop() {}
}
