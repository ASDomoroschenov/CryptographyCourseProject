package ru.mai.app.app_impl;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import ru.mai.app.app_interface.KafkaWriter;
import ru.mai.cipher.Cipher;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class KafkaWriterImpl implements KafkaWriter {
    private final Config kafkaUserConfig;
    private final KafkaProducer<byte[], byte[]> kafkaProducer;
    private final Cipher cipher;

    public KafkaWriterImpl(Config appConfig, Config kafkaUserConfig, Cipher cipher) {
        this.kafkaUserConfig = kafkaUserConfig;
        this.kafkaProducer = new KafkaProducer<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getString("kafka.producer.bootstrap.servers"),
                        ProducerConfig.CLIENT_ID_CONFIG, "producerKafkaWriter"
                ),
                new ByteArraySerializer(),
                new ByteArraySerializer()
        );
        this.cipher = cipher;
    }

    @Override
    public void processing(byte[] messageBytes) {
        ExecutorService service = Executors.newSingleThreadExecutor();

        service.submit(() -> {
            try {
                kafkaProducer.send(new ProducerRecord<>(
                        kafkaUserConfig.getString("kafka.topic.output"),
                        messageBytes
                ));
            } catch (Exception ex) {
                log.error("Error while sending message");
            }
        });
    }

    @Override
    public void close() {
        kafkaProducer.close();
    }
}
