package ru.mai.crypto.app.room.impl;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import ru.mai.crypto.app.room.KafkaWriter;

import java.util.Map;

@Slf4j
public class KafkaWriterImpl implements KafkaWriter {
    private final Config kafkaUserConfig;
    private final KafkaProducer<byte[], byte[]> kafkaProducer;

    public KafkaWriterImpl(Config appConfig, Config kafkaUserConfig) {
        this.kafkaUserConfig = kafkaUserConfig;
        this.kafkaProducer = new KafkaProducer<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getString("kafka.producer.bootstrap.servers"),
                        ProducerConfig.CLIENT_ID_CONFIG, "producerKafkaWriter"
                ),
                new ByteArraySerializer(),
                new ByteArraySerializer()
        );
    }

    @Override
    public void processing(byte[] messageBytes) {
        try {
            kafkaProducer.send(new ProducerRecord<>(
                    kafkaUserConfig.getString("kafka.topic.output"),
                    messageBytes
            ));
        } catch (Exception ex) {
            log.error("Error while sending message");
        }
    }

    @Override
    public void close() {
        kafkaProducer.close();
    }
}
