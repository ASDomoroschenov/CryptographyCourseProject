package ru.mai.app.app_impl.user;

import com.typesafe.config.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mai.app.app_impl.KafkaReaderImpl;
import ru.mai.app.app_impl.KafkaWriterImpl;
import ru.mai.app.app_interface.KafkaReader;
import ru.mai.app.app_interface.KafkaWriter;
import ru.mai.app.model.CipherInfoMessage;
import ru.mai.app.model.Message;
import ru.mai.cipher.Cipher;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class User {
    private final KafkaReader kafkaReaderBob;
    private final KafkaWriter kafkaWriterBob;
    private volatile Cipher cipher;
    @Getter
    private final long userId;
    private final BigInteger privateKey;
    private final BigInteger p;
    private final BigInteger g;

    public User(long userId, Config appConfig, Config configBob, BigInteger[] keyParameters) {
        this.userId = userId;
        this.privateKey = generatePrivateKey();
        this.p = keyParameters[0];
        this.g = keyParameters[1];
        this.kafkaReaderBob = new KafkaReaderImpl(this, appConfig, configBob, new BigInteger[]{p, privateKey});
        this.kafkaWriterBob = new KafkaWriterImpl(appConfig, configBob);
        sendCipherInfo(CipherInfoMessage.builder()
                .typeMessage("cipherInfo")
                .nameAlgorithm("RC5")
                .namePadding("ANSIX923")
                .encryptionMode("ECB")
                .sizeKeyIbBits(64)
                .sizeBlockInBits(64)
                .publicKey(generatePublicKey().toByteArray())
                .build()
        );
    }

    public void sendMessage(Message message) {
        ExecutorService service = Executors.newSingleThreadExecutor();

        service.submit(() -> {
            try {
                while (cipher == null) {
                    Thread.onSpinWait();
                }
                kafkaWriterBob.processing(cipher.encrypt(message.toBytes()));
            } catch (InterruptedException ex) {
                log.error("InterruptedException");
                log.error(Arrays.deepToString(ex.getStackTrace()));
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log.error("Error while encrypting message");
                log.error(Arrays.deepToString(ex.getStackTrace()));
            }
        });
    }

    public void sendCipherInfo(CipherInfoMessage message) {
        try {
            kafkaWriterBob.processing(message.toBytes());
        } catch (Exception ex) {
            log.error("Error while encrypting message");
        }
    }

    public void processing() {
        kafkaReaderBob.processing();
    }

    public BigInteger generatePrivateKey() {
        return new BigInteger(100, new Random());
    }

    public BigInteger generatePublicKey() {
        return g.modPow(privateKey, p);
    }

    public synchronized void setCipher(Cipher cipher) {
        this.cipher = cipher;
    }
}
