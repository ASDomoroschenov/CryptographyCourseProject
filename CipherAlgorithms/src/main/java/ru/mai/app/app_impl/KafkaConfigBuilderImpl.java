package ru.mai.app.app_impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import ru.mai.app.app_interface.KafkaConfigBuilder;

public class KafkaConfigBuilderImpl implements KafkaConfigBuilder {
    @Override
    public Config build(String topicInput, String topicOutput, String consumerGroupId, String offset) {
        return ConfigFactory.empty()
                .withValue("kafka.topic.input", ConfigValueFactory.fromAnyRef(topicInput))
                .withValue("kafka.topic.output", ConfigValueFactory.fromAnyRef(topicOutput))
                .withValue("kafka.consumer.group.id", ConfigValueFactory.fromAnyRef(consumerGroupId))
                .withValue("kafka.consumer.auto.offset.reset", ConfigValueFactory.fromAnyRef(offset));
    }
}
