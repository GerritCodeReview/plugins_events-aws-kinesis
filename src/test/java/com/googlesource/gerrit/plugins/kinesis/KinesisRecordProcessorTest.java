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

import com.gerritforge.gerrit.eventbroker.EventGsonProvider;
import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

@RunWith(MockitoJUnitRunner.class)
public class KinesisRecordProcessorTest {
  private KinesisRecordProcessor objectUnderTest;
  private Gson gson = new EventGsonProvider().get();

  @Mock Consumer<EventMessage> succeedingConsumer;
  @Mock OneOffRequestContext oneOffCtx;
  @Mock ManualRequestContext requestContext;

  @Before
  public void setup() {
    when(oneOffCtx.open()).thenReturn(requestContext);
    objectUnderTest = new KinesisRecordProcessor(succeedingConsumer, oneOffCtx, gson);
  }

  @Test
  public void shouldSkipEventWithoutSourceInstanceId() {
    Event event = new ProjectCreatedEvent();
    EventMessage messageWithoutSourceInstanceId =
        new EventMessage(new EventMessage.Header(UUID.randomUUID(), (String) null), event);

    Record kinesisRecord =
        Record.builder()
            .data(SdkBytes.fromUtf8String(gson.toJson(messageWithoutSourceInstanceId)))
            .build();
    ProcessRecordsInput kinesisInput =
        ProcessRecordsInput.builder()
            .records(Collections.singletonList(KinesisClientRecord.fromRecord(kinesisRecord)))
            .build();

    objectUnderTest.processRecords(kinesisInput);

    verify(succeedingConsumer, never()).accept(messageWithoutSourceInstanceId);
  }
}
