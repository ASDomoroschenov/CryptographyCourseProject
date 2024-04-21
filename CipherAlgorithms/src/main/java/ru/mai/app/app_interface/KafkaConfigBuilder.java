package ru.mai.app.app_interface;

import com.typesafe.config.Config;

public interface KafkaConfigBuilder {
    public Config build(String topicInput, String topicOutput, String consumerGroupId, String offset);
}
