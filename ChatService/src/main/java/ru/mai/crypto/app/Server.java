package ru.mai.crypto.app;

import org.springframework.stereotype.Service;
import ru.mai.crypto.room.room_client.RoomClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class Server {
    private static final Random random = new Random();
    private Map<String, List<RoomClient>> clients;

    public boolean createClient(String name) {
        if (clients.containsKey(name)) {
            return false;
        }

        clients.put(name, new ArrayList<>());

        return true;
    }

    public void addRoomClient(String nameClient, RoomClient roomClient) {

    }
}
