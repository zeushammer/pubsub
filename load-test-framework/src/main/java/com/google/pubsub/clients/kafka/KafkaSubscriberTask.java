// Copyright 2016 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////
package com.google.pubsub.clients.kafka;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableMap;
import com.google.pubsub.clients.common.LoadTestRunner;
import com.google.pubsub.clients.common.MetricsHandler;
import com.google.pubsub.clients.common.Task;
import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Runs a task that consumes messages utilizing Kafka's implementation of the Consumer<K,V>
 * interface.
 */
class KafkaSubscriberTask extends Task {
  private final long pollLength;
  private final KafkaConsumer<String, String> subscriber;

  private KafkaSubscriberTask(String broker, String project, String topic, long pollLength) {
    super(project, "kafka", MetricsHandler.MetricName.END_TO_END_LATENCY);
    this.pollLength = pollLength;

    // Create subscriber
    Properties props = new Properties();
    props.putAll(ImmutableMap.of(
        "key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id", "SUBSCRIBER_ID",
        "enable.auto.commit", "true",
        "session.timeout.ms", "30000"
    ));
    props.put("bootstrap.servers", broker);
    subscriber = new KafkaConsumer<>(props);
    subscriber.subscribe(Collections.singletonList(topic));
  }

  public static void main(String[] args) throws Exception {
    LoadTestRunner.Options options = new LoadTestRunner.Options();
    new JCommander(options, args);
    LoadTestRunner.run(options, request ->
        new KafkaSubscriberTask(request.getKafkaOptions().getBroker(), request.getProject(),
            request.getTopic(), request.getRequestRate()));
  }

  @Override
  public void run() {
    ConsumerRecords<String, String> records = subscriber.poll(pollLength);
    addNumberOfMessages(records.count());
    long now = System.currentTimeMillis();
    records.forEach(record -> metricsHandler.recordLatency(now - record.timestamp()));
  }
}
