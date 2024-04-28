package ru.mai.crypto.app;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import ru.mai.crypto.room.room_client.RoomClient;
import ru.mai.crypto.room.view.RoomClientView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ServerRoom {
    private static final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Map<Integer, Pair<RoomClient, RoomClient>> roomClients = new HashMap<>();
    private static final Set<String> openWindows = new HashSet<>();
    private final Server server;

    public ServerRoom(Server server) {
        this.server = server;
    }

    public RoomClient connect(String name, int roomId, RoomClientView userView) {
        if (canConnect(name, roomId)) {
            RoomClient roomClient = server.connectToRoom(name, roomId, userView);
            roomClient.setServerRoom(this);

            if (roomClients.containsKey(roomId)) {
                Pair<RoomClient, RoomClient> room = roomClients.get(roomId);
                RoomClient anotherRoomClient = room.getLeft() == null ? room.getRight() : room.getLeft();

                roomClient.setOutputTopic(anotherRoomClient.getInputTopic());
                anotherRoomClient.setOutputTopic(roomClient.getInputTopic());

                roomClients.put(roomId, Pair.of(anotherRoomClient, roomClient));

                startRoom(roomClient, anotherRoomClient);
            } else {
                roomClients.put(roomId, Pair.of(roomClient, null));
            }

            return roomClient;
        }

        return null;
    }

    public boolean canConnect(String name, int roomId) {
        if (roomClients.containsKey(roomId)) {
            Pair<RoomClient, RoomClient> room = roomClients.get(roomId);

            if (room.getLeft() == null || room.getRight() == null) {
                if (room.getLeft() != null) {
                    return !room.getLeft().getName().equals(name);
                } else {
                    return !room.getRight().getName().equals(name);
                }
            } else {
                return false;
            }
        }

        return true;
    }

    public void disconnect(String name, int roomId) {
        Pair<RoomClient, RoomClient> room = roomClients.get(roomId);

        if (room != null) {
            if (room.getLeft() == null || room.getRight() == null) {
                roomClients.remove(roomId);
            } else {
                if (room.getLeft().getName().equals(name)) {
                    roomClients.put(roomId, Pair.of(null, room.getRight()));
                } else {
                    roomClients.put(roomId, Pair.of(room.getLeft(), null));
                }
            }
        }

        server.disconnectFromRoom(name, roomId);
    }

    private void startRoom(RoomClient alice, RoomClient bob) {
        if (!alice.isRunning()) {
            alice.setRunning(true);
            service.submit(alice::processing);
        }

        if (!bob.isRunning()) {
            bob.setRunning(true);
            service.submit(bob::processing);
        }

        alice.sendCipherInfo();
        bob.sendCipherInfo();
    }

    public void addWindow(String url) {
        openWindows.add(url);
    }

    public void removeWindow(String url) {
        openWindows.remove(url);
    }

    public boolean isNotOpenWindow(String url) {
        return !openWindows.contains(url);
    }
}