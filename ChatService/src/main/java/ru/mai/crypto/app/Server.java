package ru.mai.crypto.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.mai.crypto.room.model.CipherInfoMessage;
import ru.mai.crypto.room.room_client.RoomClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class Server {
    private static final Map<String, List<RoomClient>> clients = new HashMap<>();
    private static final Map<String, CipherInfoMessage> cipherInfoClients = new HashMap<>();

    public boolean createClient(String name) {
        if (clients.containsKey(name)) {
            return false;
        }

        clients.put(name, new ArrayList<>());

        return true;
    }

    public void addRoomClient(String nameClient, RoomClient roomClient) {
        List<RoomClient> rooms = clients.get(nameClient);
        rooms.add(roomClient);
        clients.put(nameClient, rooms);
    }

    public void leaveRoom(String nameClient, int roomId) {
        List<RoomClient> rooms = clients.get(nameClient);

        for (RoomClient room : rooms) {
            if (room.getRoomId() == roomId) {
                String urlRoom = "room/" + nameClient + "/" + roomId;
                log.info("Trying to leave room...");
                room.leaveRoom();

                try {
                    room.getUi().getPage().executeJs("window.close();", urlRoom);
                } catch (NullPointerException ex) {
                    log.info("Room already closed");
                }

                rooms.remove(room);

                return;
            }
        }

        log.error("There is no such room");
    }

    public void addCipherInfo(String name, String nameAlgorithm) {
        switch (nameAlgorithm) {
            case "LOKI97" -> cipherInfoClients.put(name, createLOKI());
            case "RC5" -> cipherInfoClients.put(name, createRC5());
            default -> throw new IllegalStateException("Unexpected value: " + nameAlgorithm);
        }
    }

    public CipherInfoMessage getCipherInfo(String nameClient) {
        return cipherInfoClients.get(nameClient);
    }

    public CipherInfoMessage createLOKI() {
        return CipherInfoMessage.builder()
                .typeMessage("cipherInfo")
                .nameAlgorithm("LOKI97")
                .namePadding("ANSIX923")
                .encryptionMode("ECB")
                .sizeKeyInBits(128)
                .sizeBlockInBits(128)
                .publicKey(null)
                .initializationVector(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16})
                .build();
    }

    public CipherInfoMessage createRC5() {
        return CipherInfoMessage.builder()
                .typeMessage("cipherInfo")
                .nameAlgorithm("RC5")
                .namePadding("ANSIX923")
                .encryptionMode("ECB")
                .sizeKeyInBits(64)
                .sizeBlockInBits(64)
                .publicKey(null)
                .initializationVector(new byte[] {1, 2, 3, 4, 5, 6, 7, 8})
                .build();
    }
}