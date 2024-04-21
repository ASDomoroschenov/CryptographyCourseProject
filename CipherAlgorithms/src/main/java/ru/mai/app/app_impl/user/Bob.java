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
public class Bob implements User {
    private final KafkaReader kafkaReaderBob;
    private final KafkaWriter kafkaWriterBob;
    private final Cipher cipher;
    private final long userId;

    public Bob(long userId, Config appConfig, Config configBob, byte[] key, byte[] initializationVector) {
        this.userId = userId;
        this.cipher = new Cipher(
                initializationVector,
                new RC5(64, 64, 16, key),
                Cipher.PaddingMode.ANSIX923,
                Cipher.EncryptionMode.ECB
        );
        this.kafkaReaderBob = new KafkaReaderImpl(appConfig, configBob, cipher);
        this.kafkaWriterBob = new KafkaWriterImpl(appConfig, configBob, cipher);
    }

    @Override
    public void sendMessage(Message message) {
        try {
            kafkaWriterBob.processing(cipher.encrypt(message.toBytes()));
        } catch (Exception ex) {
            log.error("Error while encrypting message");
        }
    }

    @Override
    public void processing() {
        kafkaReaderBob.processing();
    }

    @Override
    public long getUserId() {
        return userId;
    }
}