package ru.mai.crypto.app.room;

public interface KafkaReader {
    public void processing();
    public void close();
}
