package ru.mai.crypto.app.room;

public interface KafkaWriter {
    public void processing(byte[] messageBytes);

    public void close();
}
