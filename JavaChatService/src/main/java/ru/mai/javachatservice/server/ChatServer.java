package ru.mai.javachatservice.server;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import ru.mai.javachatservice.cipher.diffie_hellman.DiffieHellman;
import ru.mai.javachatservice.model.CipherInfo;
import ru.mai.javachatservice.model.Client;
import ru.mai.javachatservice.model.Room;
import ru.mai.javachatservice.repository.CipherInfoRepository;
import ru.mai.javachatservice.repository.ClientRepository;
import ru.mai.javachatservice.repository.RoomRepository;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class ChatServer {
    private static final Map<Long, Pair<Long, Long>> roomsConnection = new HashMap<>();
    private static final Random RANDOM = new Random();
    private final CipherInfoRepository cipherInfoRepository;
    private final ClientRepository clientRepository;
    private final RoomRepository roomRepository;

    public ChatServer(CipherInfoRepository cipherInfoRepository, ClientRepository clientRepository, RoomRepository roomRepository) {
        this.cipherInfoRepository = cipherInfoRepository;
        this.clientRepository = clientRepository;
        this.roomRepository = roomRepository;
    }

    public Client authorization(String name, String nameAlgorithm) {
        CipherInfo cipherInfo = getCipherInfo(nameAlgorithm);
        return getNewClient(name, cipherInfoRepository.save(cipherInfo));
    }

    public Room createRoom(int roomId) {
        return getNewRoom();
    }

    public Room connectToRoom(Client client, long roomId) {
        if (roomsConnection.containsKey(roomId)) {
            Pair<Long, Long> roomClients = roomsConnection.get(roomId);
            Optional<Room> roomOptional = roomRepository.findById(roomId);

            if (roomOptional.isPresent()) {
                if (roomClients.getLeft() == null || roomClients.getRight() == null) {
                    if (client.getId() != roomClients.getLeft() && client.getId() != roomClients.getRight()) {
                        if (roomClients.getLeft() == null) {
                            roomsConnection.put(roomId, Pair.of(client.getId(), roomClients.getRight()));
                        } else {
                            roomsConnection.put(roomId, Pair.of(roomClients.getLeft(), client.getId()));
                        }

                        Client updateClient = clientRepository.addRoom(client.getId(), roomId);

                        if (updateClient == null) {
                            return null;
                        }

                        return roomOptional.get();
                    }
                }
            } else {
                return null;
            }
        }

        return null;
    }

    public boolean disconnectFromRoom(Client client, long roomId) {
        if (roomsConnection.containsKey(roomId)) {
            Pair<Long, Long> roomClients = roomsConnection.get(roomId);

            if (roomClients.getLeft() == client.getId()) {
                roomsConnection.put(roomId, Pair.of(null, roomClients.getRight()));
            } else if (roomClients.getRight() == client.getId()) {
                roomsConnection.put(roomId, Pair.of(roomClients.getLeft(), null));
            }
        }

        return false;
    }

    public Room getNewRoom() {
        BigInteger[] roomParams = DiffieHellman.generateParameters(300);
        Room room = Room.builder().p(roomParams[0].toByteArray()).g(roomParams[1].toByteArray()).build();
        return roomRepository.save(room);
    }

    private Client getNewClient(String name, CipherInfo cipherInfo) {
        return clientRepository.save(Client.builder()
                .name(name)
                .idCipherInfo(cipherInfo.getId())
                .rooms(null)
                .build()
        );
    }

    private CipherInfo getCipherInfo(String nameAlgorithm) {
        return switch (nameAlgorithm) {
            case "RC5" -> CipherInfo.builder()
                    .nameAlgorithm("RC5")
                    .namePadding("ANSIX923")
                    .encryptionMode("ECB")
                    .sizeKeyInBits(64)
                    .sizeBlockInBits(64)
                    .initializationVector(generateInitVector(8))
                    .build();
            case "LOKI97" -> CipherInfo.builder()
                    .nameAlgorithm("LOKI97")
                    .namePadding("ANSIX923")
                    .encryptionMode("ECB")
                    .sizeKeyInBits(128)
                    .sizeBlockInBits(128)
                    .initializationVector(generateInitVector(16))
                    .build();
            default -> throw new IllegalStateException("Unexpected value: " + nameAlgorithm);
        };
    }

    private byte[] generateInitVector(int size) {
        byte[] vector = new byte[size];

        for (int i = 0; i < size; i++) {
            vector[i] = (byte) RANDOM.nextInt(127);
        }

        return vector;
    }
}
