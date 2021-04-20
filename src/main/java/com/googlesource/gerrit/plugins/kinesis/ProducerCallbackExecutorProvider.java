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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gerrit.server.logging.LoggingContextAwareExecutorService;
import com.google.inject.Provider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ProducerCallbackExecutorProvider implements Provider<ExecutorService> {

  @Override
  public ExecutorService get() {
    // Currently, we use 1 thread only when publishing, so it makes sense to
    // have the same when processing the related callbacks.
    return new LoggingContextAwareExecutorService(
        Executors.newFixedThreadPool(
            1,
            new ThreadFactoryBuilder()
                .setNameFormat("kinesis-producer-callback-executor-%d")
                .build()));
  }
}
