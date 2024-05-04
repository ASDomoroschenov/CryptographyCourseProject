package ru.mai.testkafka;

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class Kafka {
    private final ConcurrentKafkaListenerContainerFactory<String, String> factory;

    public Kafka(ConcurrentKafkaListenerContainerFactory<String, String> factory) {
        this.factory = factory;
    }

    public void start() {
        String topic = "test_spring_message";
        System.out.println("Start");

        ConcurrentMessageListenerContainer<String, String> container = factory.createContainer(topic);
        container.setupMessageListener((MessageListener<String, String>) record -> {
            System.out.println("get message");
            System.out.println(record.value());
        });
        container.start();
    }
}
