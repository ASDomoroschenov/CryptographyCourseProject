package ru.mai.app.app_interface;

public interface KafkaWriter {
    public void processing(byte[] messageBytes);

    public void close();
}
