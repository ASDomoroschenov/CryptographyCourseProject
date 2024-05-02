package ru.mai.javachatservice.kafka;

public interface KafkaReader {
    public void processing(String inputTopic);
    public void close();
}
