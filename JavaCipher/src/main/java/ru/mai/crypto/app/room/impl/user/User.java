package ru.mai.crypto.app.room.impl.user;

import com.typesafe.config.Config;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;
import ru.mai.crypto.app.room.KafkaReader;
import ru.mai.crypto.app.room.KafkaWriter;
import ru.mai.crypto.app.room.impl.KafkaReaderImpl;
import ru.mai.crypto.app.room.impl.KafkaWriterImpl;
import ru.mai.crypto.app.room.model.CipherInfoMessage;
import ru.mai.crypto.app.room.model.Message;
import ru.mai.crypto.cipher.Cipher;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class User {
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final KafkaReader kafkaReader;
    private final KafkaWriter kafkaWriter;
    private volatile Cipher cipher;
    private final BigInteger privateKey;
    private final BigInteger p;
    private final BigInteger g;
    @Setter
    @Getter
    private String urlUser;

    public User(Config appConfig, Config userConfig, BigInteger[] keyParameters) {
        this.privateKey = generatePrivateKey();
        this.p = keyParameters[0];
        this.g = keyParameters[1];
        this.kafkaReader = new KafkaReaderImpl(this, appConfig, userConfig, new BigInteger[]{p, privateKey});
        this.kafkaWriter = new KafkaWriterImpl(appConfig, userConfig);
    }

    public void sendMessage(Message message) {
        service.submit(() -> {
            try {
                while (cipher == null) {
                    Thread.onSpinWait();
                }
                kafkaWriter.processing(cipher.encrypt(message.toBytes()));
                return true;
            } catch (InterruptedException ex) {
                log.error("InterruptedException");
                log.error(Arrays.deepToString(ex.getStackTrace()));
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log.error("Error while encrypting message");
                log.error(Arrays.deepToString(ex.getStackTrace()));
            }

            return false;
        });
    }

    public void sendCipherInfo() {
        CipherInfoMessage cipherMessage = CipherInfoMessage.builder()
                .typeMessage("cipherInfo")
                .nameAlgorithm("RC5")
                .namePadding("ANSIX923")
                .encryptionMode("ECB")
                .sizeKeyIbBits(64)
                .sizeBlockInBits(64)
                .publicKey(generatePublicKey().toByteArray())
                .build();

        service.submit(() -> {
            try {
                log.info("Send cipher info");
                kafkaWriter.processing(cipherMessage.toBytes());
            } catch (Exception ex) {
                log.error("Error while encrypting message");
            }
        });
    }

    public void processing() {
        service.submit(kafkaReader::processing);
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

    public void close() {
        kafkaReader.close();
        kafkaWriter.close();
    }

    public void showMessage(Message message) {
        try {
            String urlWithMessage = urlUser + "/show";
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForObject(urlWithMessage, message, RedirectView.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
