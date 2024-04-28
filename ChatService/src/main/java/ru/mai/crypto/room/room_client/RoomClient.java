package ru.mai.crypto.room.room_client;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.mai.crypto.app.Server;
import ru.mai.crypto.app.ServerRoom;
import ru.mai.crypto.cipher.Cipher;
import ru.mai.crypto.room.kafka.KafkaReader;
import ru.mai.crypto.room.kafka.KafkaWriter;
import ru.mai.crypto.room.model.Message;
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
@Builder
public class RoomClient {
    private static final String received = "received";
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
    private volatile Cipher cipher;
    private KafkaReader kafkaReader;
    private VerticalLayout messageLayout;
    private UI ui;
    private ServerRoom serverRoom;
    private int roomId;
    private BigInteger modulo;
    private boolean isRunning;
    private Server server;

    public boolean sendMessage(Message message) {
        try {
            if (cipher != null) {
                kafkaWriter.processing(cipher.encrypt(message.toBytes()), outputTopic);
                server.saveMessage(name, String.valueOf(roomId), message, "send");
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

    public void setUi(UI ui) {
        this.ui = ui;
    }

    public void setCipher(byte[] anotherPublicKey) {
        this.cipher = server.buildCipher(name, anotherPublicKey, privateKey, parameters[0]);
    }

    public void sendCipherInfo() {
        kafkaWriter.processing(server.buildCipherInfoMessage(name, roomId).toBytes(), outputTopic);
    }

    public void sendDeleteMessage(Message message) {
        kafkaWriter.processing(message.toBytes(), outputTopic);
    }

    public void deleteMessage(int index) {
        CompletableFuture.runAsync(() -> userView.deleteMessage(ui, index, messageLayout));
    }

    public void showMessage(Message message) {
        CompletableFuture.runAsync(() -> userView.showMessage(ui, message, messageLayout));
        server.saveMessage(name, String.valueOf(roomId), message, received);
    }

    public void showImage(Message message) {
        CompletableFuture.runAsync(() -> userView.showImage(ui, message, messageLayout));
        server.saveMessage(name, String.valueOf(roomId), message, received);
    }

    public void showFile(Message message) {
        CompletableFuture.runAsync(() -> userView.showFile(ui, message, messageLayout));
        server.saveMessage(name, String.valueOf(roomId), message, received);
    }

    public void processing() {
        kafkaReader.processing(inputTopic);
    }

    public void leaveRoom() {
        isRunning = false;
        service.submit(() -> kafkaReader.close());
        serverRoom.disconnect(name, roomId);
    }
}
