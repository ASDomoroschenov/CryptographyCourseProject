package ru.mai.app.app_impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import ru.mai.app.app_impl.user.User;
import ru.mai.app.app_interface.KafkaReader;
import ru.mai.app.model.CipherInfoMessage;
import ru.mai.app.model.Message;
import ru.mai.cipher.Cipher;
import ru.mai.cipher.cipher_impl.LOKI97;
import ru.mai.cipher.cipher_impl.RC5;
import ru.mai.cipher.cipher_interface.CipherService;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class KafkaReaderImpl implements KafkaReader {
    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
    private static final Random RANDOM = new Random();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final User user;
    private final KafkaConsumer<byte[], byte[]> kafkaConsumer;
    private final BigInteger privateKey;
    private final BigInteger modulo;

    public KafkaReaderImpl(User user, Config appConfig, Config kafkaUserConfig, BigInteger[] keyParameters) {
        this.user = user;
        this.kafkaConsumer = new KafkaConsumer<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getString("kafka.consumer.bootstrap.servers"),
                        ConsumerConfig.GROUP_ID_CONFIG, kafkaUserConfig.getString("kafka.consumer.group.id"),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaUserConfig.getString("kafka.consumer.auto.offset.reset")
                ),
                new ByteArrayDeserializer(),
                new ByteArrayDeserializer()
        );
        this.modulo = keyParameters[0];
        this.privateKey = keyParameters[1];

        TopicPartition partition = new TopicPartition(kafkaUserConfig.getString("kafka.topic.input"), 0);
        kafkaConsumer.assign(Collections.singletonList(partition));
        kafkaConsumer.seek(partition, getLastOffset(kafkaConsumer, partition));
    }

    @Override
    public void processing() {
        ExecutorService service = Executors.newSingleThreadExecutor();

        service.submit(() -> {
            boolean isRunning = true;
            Cipher cipher = null;

            try {
                while (isRunning) {
                    ConsumerRecords<byte[], byte[]> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<byte[], byte[]> consumerRecord : consumerRecords) {
                        String jsonMessage = new String(consumerRecord.value());

                        if (jsonMessage.contains("cipherInfo")) {
                            cipher = parseCipherInfoMessage(jsonMessage);
                            user.setCipher(cipher);
                        } else if (jsonMessage.contains("exit")) {
                            isRunning = false;
                        } else {
                            if (cipher != null) {
                                String test = new String(cipher.decrypt(consumerRecord.value()));
                                Message message = parseMessage(test);

                                if (message != null && message.getBytes() != null) {
                                    log.info("User {} get message: {}", user.getUserId(),  new String(message.getBytes()));
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
        });
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
        byte[] key = getKey(cipherInfo.getPublicKey(), cipherInfo.getSizeKeyIbBits());
        byte[] initializationVector = generateInitializationVector(cipherInfo.getSizeBlockInBits());

        CipherService cipherService = getCipherService(
                cipherInfo.getNameAlgorithm(),
                key,
                cipherInfo.getSizeKeyIbBits(),
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

    private static long getLastOffset(KafkaConsumer<byte[], byte[]> consumer, TopicPartition partition) {
        consumer.seekToEnd(Collections.singletonList(partition));
        long lastOffset = consumer.position(partition);
        return Math.max(0, lastOffset - 1);
    }
}
