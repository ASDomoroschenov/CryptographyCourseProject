package ru.mai.javachatservice.kafka.impl;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.stereotype.Service;
import ru.mai.javachatservice.kafka.KafkaWriter;

import java.util.Map;

@Slf4j
@Service
public class KafkaWriterImpl implements KafkaWriter {
    private static final Config appConfig = new ConfigReaderImpl().loadConfig();
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
            log.error("Error while sending message");
        }
    }

    @Override
    public void close() {
        kafkaProducer.close();
    }
}
