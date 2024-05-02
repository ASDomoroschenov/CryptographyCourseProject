package ru.mai.javachatservice.server;

import org.springframework.stereotype.Service;
import ru.mai.javachatservice.cipher.diffie_hellman.DiffieHellman;
import ru.mai.javachatservice.model.client.CipherInfo;
import ru.mai.javachatservice.model.client.ClientInfo;
import ru.mai.javachatservice.model.client.RoomInfo;
import ru.mai.javachatservice.repository.CipherInfoRepository;
import ru.mai.javachatservice.repository.ClientRepository;
import ru.mai.javachatservice.repository.RoomRepository;

import java.math.BigInteger;
import java.util.*;

@Service
public class ChatServer {
    private static final Random RANDOM = new Random();
    private static final Set<String> openTabs = new HashSet<>();
    private final CipherInfoRepository cipherInfoRepository;
    private final ClientRepository clientRepository;
    private final RoomRepository roomRepository;

    public ChatServer(CipherInfoRepository cipherInfoRepository, ClientRepository clientRepository, RoomRepository roomRepository) {
        this.cipherInfoRepository = cipherInfoRepository;
        this.clientRepository = clientRepository;
        this.roomRepository = roomRepository;
    }

    public ClientInfo authorization(String name, String nameAlgorithm) {
        CipherInfo cipherInfo = getCipherInfo(nameAlgorithm);
        return getNewClient(name, cipherInfoRepository.save(cipherInfo));
    }

    public ClientInfo getClient(long clientId) {
        return clientRepository.findById(clientId).orElse(null);
    }

    public void addTab(String url) {
        openTabs.add(url);
    }

    public void removeTab(String url) {
        openTabs.remove(url);
    }

    public boolean isNotOpenTab(String url) {
        return !openTabs.contains(url);
    }

    private RoomInfo getNewRoom(long roomId) {
        BigInteger[] roomParams = DiffieHellman.generateParameters(300);
        RoomInfo room = RoomInfo.builder()
                .id(roomId)
                .p(roomParams[0].toByteArray())
                .g(roomParams[1].toByteArray())
                .build();
        return roomRepository.save(room);
    }

    private ClientInfo getNewClient(String name, CipherInfo cipherInfo) {
        return clientRepository.save(ClientInfo.builder()
                .name(name)
                .idCipherInfo(cipherInfo.getId())
                .rooms(new long[0])
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
