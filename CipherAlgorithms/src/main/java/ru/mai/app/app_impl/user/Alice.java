package ru.mai.app.app_impl.user;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import ru.mai.app.app_impl.KafkaReaderImpl;
import ru.mai.app.app_impl.KafkaWriterImpl;
import ru.mai.app.app_interface.KafkaReader;
import ru.mai.app.app_interface.KafkaWriter;
import ru.mai.app.app_interface.user.User;
import ru.mai.app.model.Message;
import ru.mai.cipher.Cipher;
import ru.mai.cipher.cipher_impl.RC5;

@Slf4j
public class Alice implements User {
    private final KafkaReader kafkaReaderAlice;
    private final KafkaWriter kafkaWriterAlice;
    private final Cipher cipher;
    private final long userId;

    public Alice(long userId, Config appConfig, Config configAlice, byte[] key, byte[] initializationVector) {
        this.userId = userId;
        this.cipher = new Cipher(
                initializationVector,
                new RC5(64, 64, 16, key),
                Cipher.PaddingMode.ANSIX923,
                Cipher.EncryptionMode.ECB
        );
        this.kafkaReaderAlice = new KafkaReaderImpl(appConfig, configAlice, cipher);
        this.kafkaWriterAlice = new KafkaWriterImpl(appConfig, configAlice, cipher);
    }

    @Override
    public void sendMessage(Message message) {
        try {
            kafkaWriterAlice.processing(cipher.encrypt(message.toBytes()));
        } catch (Exception ex) {
            log.error("Error while encrypting message");
        }
    }

    @Override
    public void processing() {
        kafkaReaderAlice.processing();
    }

    @Override
    public long getUserId() {
        return userId;
    }
}
