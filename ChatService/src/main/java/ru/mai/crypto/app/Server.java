package ru.mai.crypto.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.mai.crypto.room.room_client.RoomClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class Server {
    private static final Map<String, List<RoomClient>> clients = new HashMap<>();

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

        log.error("Такой комнаты не существует");
    }
}