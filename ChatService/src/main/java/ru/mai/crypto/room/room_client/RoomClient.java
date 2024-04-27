package ru.mai.crypto.room.room_client;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.mai.crypto.cipher.Cipher;
import ru.mai.crypto.app.ServerRoom;
import ru.mai.crypto.cipher.cipher_interface.CipherService;
import ru.mai.crypto.room.kafka.KafkaReader;
import ru.mai.crypto.room.kafka.KafkaWriter;
import ru.mai.crypto.room.kafka.impl.KafkaReaderImpl;
import ru.mai.crypto.room.model.CipherInfoMessage;
import ru.mai.crypto.room.model.Message;
import ru.mai.crypto.room.model.parser.CipherInfoParser;
import ru.mai.crypto.room.view.RoomClientView;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
@Slf4j
public class RoomClient {
    private static final ExecutorService service = Executors.newSingleThreadExecutor();
    private static final Random random = new Random();
    private String name;
    private String outputTopic;
    private String inputTopic;
    private KafkaWriter kafkaWriter;
    private BigInteger[] parameters;
    private RoomClientView userView;
    private BigInteger privateKey;
    private BigInteger publicKey;
    private volatile byte[] anotherPublicKey = null;
    private volatile Cipher cipher;
    private KafkaReader kafkaReader;
    private VerticalLayout messageLayout;
    private UI ui;
    private ServerRoom serverRoom;
    private CipherInfoMessage cipherInfo;
    private int roomId;
    private BigInteger modulo;
    private BigInteger base;

    public RoomClient(CipherInfoMessage cipherInfo, ServerRoom serverRoom, int roomId, String name, String outputTopic, String inputTopic, KafkaWriter kafkaWriter, BigInteger[] parameters, RoomClientView userView) {
        this.cipherInfo = cipherInfo;
        this.serverRoom = serverRoom;
        this.roomId = roomId;
        this.name = name;
        this.outputTopic = outputTopic;
        this.inputTopic = inputTopic;
        this.kafkaWriter = kafkaWriter;
        this.parameters = parameters;
        this.privateKey = generatePrivateKey();
        this.publicKey = generatePublicKey(this.privateKey);
        this.userView = userView;
        this.modulo = parameters[0];
        this.kafkaReader = new KafkaReaderImpl(this, privateKey, modulo);
    }

    public boolean sendMessage(Message message) {
        try {
            if (anotherPublicKey != null) {
                log.info("Encrypted message with algorithm {}: {}", cipherInfo.getNameAlgorithm(), new String(cipher.encrypt(message.toBytes())));
                kafkaWriter.processing(cipher.encrypt(message.toBytes()), outputTopic);
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }

        return false;
    }

    public void setAnotherKey(byte[] anotherPublicKey) {
        byte[] key = CipherInfoParser.getKey(anotherPublicKey, cipherInfo.getSizeKeyInBits(), privateKey, modulo);
        byte[] initializationVector = cipherInfo.getInitializationVector();

        CipherService cipherService = CipherInfoParser.getCipherService(
                cipherInfo.getNameAlgorithm(),
                key,
                cipherInfo.getSizeKeyInBits(),
                cipherInfo.getSizeBlockInBits()
        );

        if (cipherInfo.getNameAlgorithm().equals("LOKI97")) {
            log.info(Arrays.toString(key));
            log.info(String.valueOf(cipherInfo.getSizeBlockInBits()));
            log.info(Arrays.toString(cipherInfo.getInitializationVector()));
        }

        cipher = new Cipher(
                initializationVector,
                cipherService,
                CipherInfoParser.getPadding(cipherInfo.getNamePadding()),
                CipherInfoParser.getEncryptionMode(cipherInfo.getEncryptionMode())
        );

        this.anotherPublicKey = anotherPublicKey;
    }

    public void sendCipherInfo() {
        cipherInfo.setPublicKey(publicKey.toByteArray());
        log.info("Send cipher info to {}", outputTopic);
        kafkaWriter.processing(cipherInfo.toBytes(), outputTopic);
    }

    public BigInteger generatePrivateKey() {
        return new BigInteger(100, random);
    }

    public BigInteger generatePublicKey(BigInteger privateKey) {
        return parameters[1].modPow(privateKey, parameters[0]);
    }

    public void showMessage(Message message) {
        CompletableFuture.runAsync(() -> userView.showMessage(ui, message, messageLayout));
    }

    public void showImage(Message message) {
        CompletableFuture.runAsync(() -> userView.showImage(ui, message, messageLayout));
    }

    public void showFile(Message message) {
        CompletableFuture.runAsync(() -> userView.showFile(ui, message, messageLayout));
    }

    public void processing() {
        kafkaReader.processing(inputTopic);
    }

    public void leaveRoom() {
        service.submit(() -> kafkaReader.close());
        serverRoom.leaveRoom(this);
    }
}
