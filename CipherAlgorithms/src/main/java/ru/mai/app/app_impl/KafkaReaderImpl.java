package ru.mai.app.app_impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import ru.mai.app.app_interface.KafkaReader;
import ru.mai.cipher.Cipher;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class KafkaReaderImpl implements KafkaReader {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaConsumer<byte[], byte[]> kafkaConsumer;
    private final Cipher cipher;

    public KafkaReaderImpl(Config appConfig, Config kafkaUserConfig, Cipher cipher) {
        this.kafkaConsumer = new KafkaConsumer<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getString("kafka.consumer.bootstrap.servers"),
                        ConsumerConfig.GROUP_ID_CONFIG, kafkaUserConfig.getString("kafka.consumer.group.id"),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaUserConfig.getString("kafka.consumer.auto.offset.reset")
                ),
                new ByteArrayDeserializer(),
                new ByteArrayDeserializer()
        );
        kafkaConsumer.subscribe(Collections.singletonList(kafkaUserConfig.getString("kafka.topic.input")));
        this.cipher = cipher;
    }

    @Override
    public void processing() {
        ExecutorService service = Executors.newSingleThreadExecutor();

        service.submit(() -> {
            boolean isRunning = true;

            try {
                while (isRunning) {
                    ConsumerRecords<byte[], byte[]> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<byte[], byte[]> consumerRecord : consumerRecords) {
                        String message = new String(cipher.decrypt(consumerRecord.value()));
                        System.out.println("Get message: - " + message);
                    }
                }
            } catch (Exception ex) {
                log.error("Error while decrypting message");
            }
        });
    }
}
