package ru.mai.crypto.room;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import ru.mai.crypto.app.Server;
import ru.mai.crypto.diffie_hellman.DiffieHellman;
import ru.mai.crypto.room.kafka.KafkaWriter;
import ru.mai.crypto.room.kafka.impl.KafkaWriterImpl;
import ru.mai.crypto.room.room_client.RoomClient;
import ru.mai.crypto.room.view.RoomClientView;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ServerRoom {
    private static final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final KafkaWriter kafkaWriter = new KafkaWriterImpl();
    private static final Random random = new Random();
    private static final Map<Integer, RoomClient> chatUsers = new HashMap<>();
    private static final Map<Integer, Pair<RoomClient, RoomClient>> roomUsers = new HashMap<>();
    private static final Map<Integer, BigInteger[]> roomsParameter = new HashMap<>();
    private final Server server;

    public ServerRoom(Server server) {
        this.server = server;
    }

    public RoomClient connect(String name, int roomId, RoomClientView userView) {
        int userId = generateUserId();
        BigInteger[] parameters;
        RoomClient roomClient;
        String outputTopic;
        String inputTopic = topicName(name, roomId);

        if (roomsParameter.containsKey(roomId)) {
            parameters = roomsParameter.get(roomId);
            RoomClient anotherRoomClient = roomUsers.get(roomId).getLeft();

            outputTopic = topicName(anotherRoomClient.getName(), roomId);

            anotherRoomClient.setOutputTopic(topicName(name, roomId));

            log.info("Name input topic {} from roomClient {}", inputTopic, name);
            log.info("Name output topic {} from roomClient {}", outputTopic, name);

            log.info("Name input topic {} from roomClient {}", anotherRoomClient.getInputTopic(), anotherRoomClient.getName());
            log.info("Name output topic {} from roomClient {}", anotherRoomClient.getOutputTopic(), anotherRoomClient.getName());
        } else {
            parameters = DiffieHellman.generateParameters(300);
            roomsParameter.put(roomId, parameters);

            outputTopic = null;
        }

        roomClient = new RoomClient(
                name,
                userId,
                outputTopic,
                inputTopic,
                kafkaWriter,
                parameters,
                userView
        );
        
        server.addRoomClient(name, roomClient);

        if (roomUsers.containsKey(roomId)) {
            RoomClient anotherUser = roomUsers.get(roomId).getLeft();
            startRoom(anotherUser, roomClient);
            roomUsers.put(roomId, Pair.of(anotherUser, roomClient));
        } else {
            roomUsers.put(roomId, Pair.of(roomClient, null));
        }

        chatUsers.put(userId, roomClient);

        return roomClient;
    }

    private int generateUserId() {
        int userId;

        do {
            userId = random.nextInt(Integer.MAX_VALUE);
        } while (chatUsers.containsKey(userId));

        return userId;
    }

    private String topicName(String name, int roomId) {
        return "input_" + name + "_" + roomId;
    }

    private void startRoom(RoomClient alice, RoomClient bob) {
        service.submit(alice::processing);
        service.submit(bob::processing);

        alice.sendCipherInfo();
        bob.sendCipherInfo();
    }
}