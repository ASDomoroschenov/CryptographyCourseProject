package ru.mai.crypto.app.room.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import ru.mai.crypto.app.room.KafkaWriter;

import java.util.Map;

@Slf4j
public class KafkaWriterImpl implements KafkaWriter {
    private final KafkaProducer<byte[], byte[]> kafkaProducer;

    public KafkaWriterImpl() {
        this.kafkaProducer = new KafkaProducer<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093",
                        ProducerConfig.CLIENT_ID_CONFIG, "producerKafkaWriter"
                ),
                new ByteArraySerializer(),
                new ByteArraySerializer()
        );
    }

    @Override
    public void processing(byte[] messageBytes, String outputTopic) {
        try {
            kafkaProducer.send(new ProducerRecord<>(
                    outputTopic,
                    messageBytes
            ));
        } catch (Exception ex) {
            log.info(ex.getMessage());
            log.error("Error while sending message");
        }
    }

    @Override
    public void close() {
        kafkaProducer.close();
    }
}
