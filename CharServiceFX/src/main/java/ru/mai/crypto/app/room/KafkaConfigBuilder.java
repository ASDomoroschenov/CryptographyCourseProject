package ru.mai.crypto.app.room;

import com.typesafe.config.Config;

public interface KafkaConfigBuilder {
    public Config build(String topicInput, String topicOutput, String consumerGroupId, String offset);
}
