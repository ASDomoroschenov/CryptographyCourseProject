package ru.mai.crypto.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import ru.mai.crypto.app.model.ClientCipherInfo;
import ru.mai.crypto.app.model.ClientInfo;
import ru.mai.crypto.app.model.ClientRoomInfo;
import ru.mai.crypto.cipher.Cipher;
import ru.mai.crypto.cipher.cipher_interface.CipherService;
import ru.mai.crypto.diffie_hellman.DiffieHellman;
import ru.mai.crypto.room.kafka.KafkaWriter;
import ru.mai.crypto.room.kafka.impl.ConfigReaderImpl;
import ru.mai.crypto.room.kafka.impl.KafkaReaderImpl;
import ru.mai.crypto.room.kafka.impl.KafkaWriterImpl;
import ru.mai.crypto.room.model.CipherInfoMessage;
import ru.mai.crypto.room.model.Message;
import ru.mai.crypto.room.model.parser.CipherInfoParser;
import ru.mai.crypto.room.room_client.RoomClient;
import ru.mai.crypto.room.view.RoomClientView;

import java.math.BigInteger;
import java.util.*;

@Slf4j
@Service
public class Server {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Config config = new ConfigReaderImpl().loadConfig();
    private final RoomClientFactory roomClientFactory = new RoomClientFactory(this);
    private static final Random random = new Random();
    private static final KafkaWriter kafkaWriter = new KafkaWriterImpl();
    private static final Map<String, List<RoomClient>> clients = new HashMap<>();
    private static final Map<Integer, BigInteger[]> activeRooms = new HashMap<>();
    private static final Map<String, ClientInfo> serverClients = new HashMap<>();
    private static final JedisPooled jedisPooled = new JedisPooled(config.getString("redis.host"), config.getInt("redis.port"));

    public boolean createClient(String name, String nameAlgorithm) {
        if (serverClients.containsKey(name)) {
            return false;
        }

        serverClients.put(name, new ClientInfo(
                name,
                ClientCipherInfoFactory.createClientCipherInfo(nameAlgorithm),
                new HashMap<>()));
        clients.put(name, new ArrayList<>());
        jedisPooled.set(name, "");

        try {
            log.info(OBJECT_MAPPER.writeValueAsString(serverClients.get(name)));
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }

        return true;
    }

    public RoomClient connectToRoom(String name, int roomId, RoomClientView userView) {
        BigInteger[] encryptionParam;

        if (activeRooms.containsKey(roomId)) {
            encryptionParam = activeRooms.get(roomId);
        } else {
            encryptionParam = DiffieHellman.generateParameters(300);
            activeRooms.put(roomId, encryptionParam);
        }

        RoomClient roomClient = roomClientFactory.createRoomClient(name, roomId, userView, encryptionParam);
        clients.get(name).add(roomClient);

        return roomClient;
    }

    public void disconnectFromRoom(String name, int roomId) {
        List<RoomClient> rooms = clients.get(name);

        for (RoomClient room : rooms) {
            if (room.getRoomId() == roomId) {
                String urlRoom = "room/" + name + "/" + roomId;

                try {
                    room.getUi().getPage().executeJs("window.close();", urlRoom);
                } catch (NullPointerException ex) {
                    log.info("Room already closed");
                }

                rooms.remove(room);

                serverClients.get(name).getActiveRoom().remove(roomId);

                return;
            }
        }
    }

    public Cipher buildCipher(String nameClient, byte[] publicKey, BigInteger privateKey, BigInteger modulo) {
        ClientCipherInfo cipherInfo = serverClients.get(nameClient).getClientCipherInfo();
        byte[] key = CipherInfoParser.getKey(publicKey, cipherInfo.getSizeKeyInBits(), privateKey, modulo);
        byte[] initializationVector = cipherInfo.getInitializationVector();

        CipherService cipherService = CipherInfoParser.getCipherService(
                cipherInfo.getNameAlgorithm(),
                key,
                cipherInfo.getSizeKeyInBits(),
                cipherInfo.getSizeBlockInBits()
        );

        return new Cipher(
                initializationVector,
                cipherService,
                CipherInfoParser.getPadding(cipherInfo.getNamePadding()),
                CipherInfoParser.getEncryptionMode(cipherInfo.getEncryptionMode())
        );
    }

    public CipherInfoMessage buildCipherInfoMessage(String nameClient, int roomId) {
        ClientCipherInfo clientCipherInfo = serverClients.get(nameClient).getClientCipherInfo();
        byte[] publicKey = serverClients.get(nameClient).getActiveRoom().get(roomId).getPublicKey().toByteArray();

        return CipherInfoMessage
                .builder()
                .typeMessage("cipherInfo")
                .nameAlgorithm(clientCipherInfo.getNameAlgorithm())
                .namePadding(clientCipherInfo.getNamePadding())
                .encryptionMode(clientCipherInfo.getEncryptionMode())
                .sizeKeyInBits(clientCipherInfo.getSizeKeyInBits())
                .sizeBlockInBits(clientCipherInfo.getSizeBlockInBits())
                .initializationVector(clientCipherInfo.getInitializationVector())
                .publicKey(publicKey)
                .build();
    }

    public void saveMessage(String name, String roomId, Message message, String from) {
        try {
            String jsonMessage = from + "_" + roomId + ": " + OBJECT_MAPPER.writeValueAsString(message) + System.lineSeparator();
            jedisPooled.append(name, jsonMessage);
        } catch (JsonProcessingException ex) {
            log.error("Error while parsing message");
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }
    }

    public static class ClientCipherInfoFactory {
        private ClientCipherInfoFactory() {
        }

        public static ClientCipherInfo createClientCipherInfo(String nameAlgorithm) {
            return switch (nameAlgorithm) {
                case "RC5" -> createRC5();
                case "LOKI97" -> createLOKI();
                default -> throw new IllegalStateException("Unexpected value: " + nameAlgorithm);
            };
        }

        private static ClientCipherInfo createLOKI() {
            return ClientCipherInfo.builder()
                    .nameAlgorithm("LOKI97")
                    .namePadding("ANSIX923")
                    .encryptionMode("ECB")
                    .sizeKeyInBits(128)
                    .sizeBlockInBits(128)
                    .initializationVector(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16})
                    .build();
        }

        private static ClientCipherInfo createRC5() {
            return ClientCipherInfo.builder()
                    .nameAlgorithm("RC5")
                    .namePadding("ANSIX923")
                    .encryptionMode("ECB")
                    .sizeKeyInBits(64)
                    .sizeBlockInBits(64)
                    .initializationVector(new byte[]{1, 2, 3, 4, 5, 6, 7, 8})
                    .build();
        }
    }

    public static class RoomClientFactory {
        private final Server server;

        public RoomClientFactory(Server server) {
            this.server = server;
        }

        public RoomClient createRoomClient(String name, int roomId, RoomClientView userView, BigInteger[] encryptionParam) {
            BigInteger privateKey = generatePrivateKey();
            BigInteger publicKey = generatePublicKey(privateKey, encryptionParam);
            String nameInputTopic = getTopicName(name, roomId);

            serverClients.get(name).getActiveRoom().put(
                    roomId,
                    ClientRoomInfo
                            .builder()
                            .nameInputTopic(nameInputTopic)
                            .nameOutputTopic(null)
                            .nameOutputTopic(null)
                            .encryptionParam(encryptionParam)
                            .privateKey(privateKey)
                            .publicKey(publicKey)
                            .build()
            );

            RoomClient roomClient = RoomClient.builder()
                    .server(server)
                    .roomId(roomId)
                    .name(name)
                    .inputTopic(nameInputTopic)
                    .outputTopic(null)
                    .kafkaWriter(kafkaWriter)
                    .parameters(encryptionParam)
                    .userView(userView)
                    .privateKey(privateKey)
                    .publicKey(publicKey)
                    .build();
            roomClient.setKafkaReader(new KafkaReaderImpl(roomClient, privateKey, encryptionParam[0]));

            return roomClient;
        }

        public static BigInteger generatePrivateKey() {
            return new BigInteger(100, random);
        }

        public static BigInteger generatePublicKey(BigInteger privateKey, BigInteger[] encryptionParam) {
            return encryptionParam[1].modPow(privateKey, encryptionParam[0]);
        }

        private static String getTopicName(String name, int roomId) {
            return "input_" + name + "_" + roomId;
        }
    }
}