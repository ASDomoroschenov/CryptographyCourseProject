package ru.mai.crypto.room.kafka.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import ru.mai.crypto.cipher.Cipher;
import ru.mai.crypto.cipher.cipher_impl.LOKI97;
import ru.mai.crypto.cipher.cipher_impl.RC5;
import ru.mai.crypto.cipher.cipher_interface.CipherService;
import ru.mai.crypto.room.kafka.KafkaReader;
import ru.mai.crypto.room.room_client.RoomClient;
import ru.mai.crypto.room.model.CipherInfoMessage;
import ru.mai.crypto.room.model.Message;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class KafkaReaderImpl implements KafkaReader {
    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
    private static final Random RANDOM = new Random();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final KafkaConsumer<byte[], byte[]> kafkaConsumer;
    private final BigInteger privateKey;
    private final BigInteger modulo;
    private volatile boolean isRunning = true;
    private final RoomClient roomClient;

    public KafkaReaderImpl(RoomClient roomClient, BigInteger[] keyParameters) {
        this.roomClient = roomClient;
        this.kafkaConsumer = new KafkaConsumer<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093",
                        ConsumerConfig.GROUP_ID_CONFIG, "group_" + roomClient.getName() + "_" + roomClient.getRoomId(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new ByteArrayDeserializer(),
                new ByteArrayDeserializer()
        );
        this.modulo = keyParameters[0];
        this.privateKey = keyParameters[1];
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
                        cipher = parseCipherInfoMessage(jsonMessage);
                        roomClient.setCipher(cipher);
                        log.info("Get cipher info to {}", roomClient.getName());
                    } else if (jsonMessage.contains("exit")) {
                        isRunning = false;
                    } else {
                        if (cipher != null) {
                            log.info(new String(cipher.decrypt(consumerRecord.value())));
                            Message message = parseMessage(new String(cipher.decrypt(consumerRecord.value())));

                            if (message != null && message.getBytes() != null) {
                                if (message.getTypeFormat().equals("text")) {
                                    CompletableFuture.runAsync(() -> roomClient.showMessage(message));
                                    log.info("roomClient {} get message: {}", roomClient.getName(), new String(message.getBytes()));
                                } else if (message.getTypeFormat().equals("image")) {
                                    log.info("roomClient {} get image", roomClient.getName());
                                    CompletableFuture.runAsync(() -> roomClient.showImage(message));
                                } else {
                                    log.info("roomClient {} get file", roomClient.getName());
                                    CompletableFuture.runAsync(() -> roomClient.showFile(message));
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

    private Message parseMessage(String message) {
        try {
            return OBJECT_MAPPER.readValue(message, Message.class);
        } catch (JsonProcessingException ex) {
            log.error("Error while parsing json string");
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }

        return null;
    }

    private Cipher parseCipherInfoMessage(String message) {
        try {
            return getCipher(OBJECT_MAPPER.readValue(message, CipherInfoMessage.class));
        } catch (JsonProcessingException ex) {
            log.error("Error while parsing json string");
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }

        return null;
    }

    private Cipher getCipher(CipherInfoMessage cipherInfo) {
        byte[] key = getKey(cipherInfo.getPublicKey(), cipherInfo.getSizeKeyInBits());
        byte[] initializationVector = generateInitializationVector(cipherInfo.getSizeBlockInBits());

        CipherService cipherService = getCipherService(
                cipherInfo.getNameAlgorithm(),
                key,
                cipherInfo.getSizeKeyInBits(),
                cipherInfo.getSizeBlockInBits()
        );

        return new Cipher(
                initializationVector,
                cipherService,
                getPadding(cipherInfo.getNamePadding()),
                getEncryptionMode(cipherInfo.getEncryptionMode())
        );
    }

    private byte[] getKey(byte[] publicKey, int sizeKeyInBits) {
        BigInteger publicKeyNumber = new BigInteger(publicKey);
        BigInteger key = publicKeyNumber.modPow(privateKey, modulo);
        byte[] keyBytes = key.toByteArray();
        byte[] result = new byte[sizeKeyInBits / Byte.SIZE];
        System.arraycopy(keyBytes, 0, result, 0, sizeKeyInBits / Byte.SIZE);
        return result;
    }

    private byte[] generateInitializationVector(int sizeBlockInBits) {
        byte[] initializationVector = new byte[sizeBlockInBits / Byte.SIZE];

        for (int i = 0; i < initializationVector.length; i++) {
            initializationVector[i] = (byte) RANDOM.nextInt(128);
        }

        return initializationVector;
    }

    private CipherService getCipherService(String nameAlgorithm, byte[] key, int sizeKeyInBits, int sizeBlockInBits) {
        return switch (nameAlgorithm) {
            case "LOKI97" -> new LOKI97(key, sizeKeyInBits);
            case "RC5" -> new RC5(sizeKeyInBits, sizeBlockInBits, 16, key);
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + nameAlgorithm);
        };
    }

    private Cipher.PaddingMode getPadding(String namePadding) {
        return switch (namePadding) {
            case "ANSIX923" -> Cipher.PaddingMode.ANSIX923;
            case "ISO10126" -> Cipher.PaddingMode.ISO10126;
            case "PKCS7" -> Cipher.PaddingMode.PKCS7;
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + namePadding);
        };
    }

    private Cipher.EncryptionMode getEncryptionMode(String encryptionMode) {
        return switch (encryptionMode) {
            case "CBC" -> Cipher.EncryptionMode.CBC;
            case "CFB" -> Cipher.EncryptionMode.CFB;
            case "CTR" -> Cipher.EncryptionMode.CTR;
            case "ECB" -> Cipher.EncryptionMode.ECB;
            case "OFB" -> Cipher.EncryptionMode.OFB;
            case "PCBC" -> Cipher.EncryptionMode.PCBC;
            case "RD" -> Cipher.EncryptionMode.RD;
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + encryptionMode);
        };
    }

    public void close() {
        isRunning = false;
    }
}
