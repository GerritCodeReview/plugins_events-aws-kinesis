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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.EventDeserializer;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGsonProvider;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

@RunWith(MockitoJUnitRunner.class)
public class KinesisRecordProcessorTest {
  private KinesisRecordProcessor objectUnderTest;
  private Gson gson = new EventGsonProvider().get();
  private EventDeserializer eventDeserializer = new EventDeserializer(gson);

  @Mock Consumer<Event> succeedingConsumer;
  @Captor ArgumentCaptor<Event> eventMessageCaptor;
  @Mock OneOffRequestContext oneOffCtx;
  @Mock ManualRequestContext requestContext;
  @Mock Configuration configuration;

  @Before
  public void setup() {
    when(oneOffCtx.open()).thenReturn(requestContext);
    objectUnderTest =
        new KinesisRecordProcessor(succeedingConsumer, oneOffCtx, eventDeserializer, configuration);
  }

  @Test
  public void shouldNotCheckpointBeforeIntervalIsExpired() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = UUID.randomUUID().toString();

    ProcessRecordsInput kinesisInput = sampleMessage(gson.toJson(event));
    ProcessRecordsInput processRecordsInputSpy = Mockito.spy(kinesisInput);

    objectUnderTest.processRecords(processRecordsInputSpy);

    verify(processRecordsInputSpy, never()).checkpointer();
  }

  @Test
  public void shouldCheckpointAfterIntervalIsExpired() throws InterruptedException {
    when(configuration.getCheckpointIntervalMs()).thenReturn(0L);
    Event event = new ProjectCreatedEvent();
    event.instanceId = UUID.randomUUID().toString();

    ProcessRecordsInput kinesisInput = sampleMessage(gson.toJson(event));
    ProcessRecordsInput processRecordsInputSpy = Mockito.spy(kinesisInput);

    objectUnderTest.processRecords(processRecordsInputSpy);

    verify(processRecordsInputSpy, times(1)).checkpointer();
  }

  @Test
  public void shouldSkipEventWithoutSourceInstanceId() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = UUID.randomUUID().toString();

    ProcessRecordsInput kinesisInput = sampleMessage(gson.toJson(event));

    objectUnderTest.processRecords(kinesisInput);

    verify(succeedingConsumer, never()).accept(event);
  }

  @Test
  public void shouldParseEventObject() {
    String instanceId = "instance-id";

    Event event = new ProjectCreatedEvent();
    event.instanceId = instanceId;

    ProcessRecordsInput kinesisInput = sampleMessage(gson.toJson(event));
    objectUnderTest.processRecords(kinesisInput);

    verify(succeedingConsumer, only()).accept(eventMessageCaptor.capture());

    Event result = eventMessageCaptor.getValue();
    assertThat(result.instanceId).isEqualTo(instanceId);
  }

  @Test
  public void shouldProcessEventObjectWithoutInstanceId() {
    Event event = new ProjectCreatedEvent();
    event.instanceId = null;

    ProcessRecordsInput kinesisInput = sampleMessage(gson.toJson(event));
    objectUnderTest.processRecords(kinesisInput);

    verify(succeedingConsumer, times(1)).accept(any());
  }

  @Test
  public void shouldSkipEventObjectWithUnknownType() {
    String instanceId = "instance-id";
    Event event = new Event("unknown-type") {};
    event.instanceId = instanceId;

    ProcessRecordsInput kinesisInput = sampleMessage(gson.toJson(event));
    objectUnderTest.processRecords(kinesisInput);

    verify(succeedingConsumer, never()).accept(any());
  }

  @Test
  public void shouldSkipEventObjectWithoutType() {
    String instanceId = "instance-id";
    Event event = new Event(null) {};
    event.instanceId = instanceId;

    ProcessRecordsInput kinesisInput = sampleMessage(gson.toJson(event));
    objectUnderTest.processRecords(kinesisInput);

    verify(succeedingConsumer, never()).accept(any());
  }

  @Test
  public void shouldSkipEmptyObjectJsonPayload() {
    String emptyJsonObject = "{}";

    ProcessRecordsInput kinesisInput = sampleMessage(emptyJsonObject);
    objectUnderTest.processRecords(kinesisInput);

    verify(succeedingConsumer, never()).accept(any());
  }

  @Test
  public void shouldParseEventObjectWithHeaderAndBodyProjectName() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = "instance-id";
    event.projectName = "header_body_parser_project";
    ProcessRecordsInput kinesisInput = sampleMessage(gson.toJson(event));
    objectUnderTest.processRecords(kinesisInput);

    verify(succeedingConsumer, only()).accept(any(Event.class));
  }

  private ProcessRecordsInput sampleMessage(String message) {
    Record kinesisRecord = Record.builder().data(SdkBytes.fromUtf8String(message)).build();
    ProcessRecordsInput kinesisInput =
        ProcessRecordsInput.builder()
            .records(Collections.singletonList(KinesisClientRecord.fromRecord(kinesisRecord)))
            .build();
    return kinesisInput;
  }
}
