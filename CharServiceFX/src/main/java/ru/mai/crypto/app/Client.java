package ru.mai.crypto.app;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mai.crypto.app.room.model.Message;
import ru.mai.crypto.cipher.Cipher;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class Client implements Serializable {
    private static final String HOST = "localhost";
    private static final int PORT = 8843;
    private List<Integer> roomsId = null;
    private Map<Integer, BigInteger[]> parametersRoom = null;
    @Setter
    private int clientId;
    private volatile Cipher cipher = null;

    public void connect(int roomId) {
        sendMessageToServer(Message.builder().action("connect").roomId(roomId).client(this).build());
    }

    public void createRoom(int roomId) {
        sendMessageToServer(Message.builder().action("create_room").roomId(roomId).client(this).build());
    }

    public void sendMessageToServer(Message message) {
        try (Socket socket = new Socket(HOST, PORT)) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(message);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }
    }

    public void sendMessageToClient(int roomId, String format, byte[] bytes) {
        Message message = Message
                .builder()
                .client(null)
                .clientId(clientId)
                .action("send")
                .format(format)
                .roomId(roomId)
                .bytes(bytes)
                .build();
        sendMessageToClient(message);
    }

    public void sendMessageToClient(Message message) {
        while (cipher == null) {
            Thread.onSpinWait();
        }

        try (Socket socket = new Socket(HOST, PORT)) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            message.setBytes(cipher.encrypt(message.getBytes()));
            objectOutputStream.writeObject(message);
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }
    }
}
