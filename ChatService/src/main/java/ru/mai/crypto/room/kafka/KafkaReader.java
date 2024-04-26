package ru.mai.crypto.room.kafka;

public interface KafkaReader {
    public void processing(String inputTopic);
    public void close();
}
