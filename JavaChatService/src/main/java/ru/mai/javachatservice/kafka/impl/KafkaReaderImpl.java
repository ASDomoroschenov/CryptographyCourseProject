package ru.mai.javachatservice.kafka.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import ru.mai.javachatservice.cipher.Cipher;
import ru.mai.javachatservice.kafka.KafkaReader;
import ru.mai.javachatservice.model.messages.CipherInfoMessage;
import ru.mai.javachatservice.model.messages.Message;
import ru.mai.javachatservice.model.messages.json_parser.CipherInfoMessageParser;
import ru.mai.javachatservice.model.messages.json_parser.MessageParser;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class KafkaReaderImpl implements KafkaReader {
    private static final Config appConfig = new ConfigReaderImpl().loadConfig();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final KafkaConsumer<byte[], byte[]> kafkaConsumer;
    private final BigInteger privateKey;
    private final BigInteger modulo;
    private volatile boolean isRunning = true;
    private final RoomClient roomClient;

    public KafkaReaderImpl(RoomClient roomClient, BigInteger privateKey, BigInteger modulo) {
        this.roomClient = roomClient;
        this.kafkaConsumer = new KafkaConsumer<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093",
                        ConsumerConfig.GROUP_ID_CONFIG, "group_" + roomClient.getClientId() + "_" + roomClient.getRoomId(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new ByteArrayDeserializer(),
                new ByteArrayDeserializer()
        );
        this.privateKey = privateKey;
        this.modulo = modulo;
    }

    @Override
    public void processing(String inputTopic) {
        Cipher cipher = null;
        kafkaConsumer.subscribe(Collections.singletonList(inputTopic));

        try {
            while (isRunning) {
                ConsumerRecords<byte[], byte[]> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<byte[], byte[]> consumerRecord : consumerRecords) {
                    String jsonMessage = new String(consumerRecord.value());

                    if (jsonMessage.contains("cipherInfo")) {
                        CipherInfoMessage cipherInfo = OBJECT_MAPPER.readValue(jsonMessage, CipherInfoMessage.class);
                        cipher = CipherInfoMessageParser.parseCipherInfoMessage(jsonMessage, privateKey, modulo);
                        roomClient.setCipher(cipherInfo.getPublicKey());
                        log.info("client get cipher info");
                    } else if (jsonMessage.contains("delete_message")) {
                        Message deleteMessage = OBJECT_MAPPER.readValue(jsonMessage, Message.class);
                        roomClient.deleteMessage(deleteMessage.getIndexMessage());
                    } else if (jsonMessage.contains("disconnect")) {
                        roomClient.clearMessages();
                    } else {
                        if (cipher != null) {
                            Message message = MessageParser.parseMessage(new String(cipher.decrypt(consumerRecord.value())));

                            if (message != null && message.getBytes() != null) {
                                if (message.getTypeFormat().equals("text")) {
                                    roomClient.showMessage(message);
                                } else if (message.getTypeFormat().equals("image")) {
                                    roomClient.showImage(message);
                                } else {
                                    roomClient.showFile(message);
                                }
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException ex) {
            log.error("Error working kafka reader");
            log.error(Arrays.deepToString(ex.getStackTrace()));
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.error("Error working kafka reader");
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }

        log.info("End kafka reader roomClient");
        kafkaConsumer.close();
    }

    public void close() {
        isRunning = false;
    }
}
