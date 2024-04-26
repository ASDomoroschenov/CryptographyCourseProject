package ru.mai.crypto.app;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.crypto.app.room.KafkaReader;
import ru.mai.crypto.app.room.KafkaWriter;
import ru.mai.crypto.app.room.impl.KafkaReaderImpl;
import ru.mai.crypto.app.room.impl.KafkaWriterImpl;
import ru.mai.crypto.app.room.model.CipherInfoMessage;
import ru.mai.crypto.app.room.model.Message;
import ru.mai.crypto.cipher.diffie_hellman.DiffieHellman;

import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Server {
    private static final String TOPIC_PREFIX = "input_";
    private static final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final KafkaWriter kafkaWriter = new KafkaWriterImpl();
    private static final Map<String, String> outputTopics = new HashMap<>();
    private static final Map<Integer, Pair<Integer, Integer>> rooms = new HashMap<>();
    private static final Map<Integer, Pair<Client, Client>> clients = new HashMap<>();
    private static final Random random = new Random();
    @Setter
    private volatile boolean isAlive = true;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(8843)) {
            log.info("Сервер запущен");
            log.info("Ожидание подключения...");

            while (isAlive) {
                handle(serverSocket);
            }
        } catch (Exception ex) {
            log.error("Error while working server");
            log.info(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }
    }

    private static void handle(ServerSocket serverSocket) {
        try (Socket clientSocket = serverSocket.accept()) {
            log.info("Клиент подключился");

            ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());

            Message message = (Message) objectInputStream.readObject();

            if (message.getAction().equals("create_room")) {
                int roomId = random.nextInt(Integer.MAX_VALUE);
                log.info("Room {} created", roomId);
            }

            if (message.getAction().equals("connect")) {
                int roomId = message.getRoomId();

                if (rooms.containsKey(roomId)) {
                    int aliceId = rooms.get(roomId).getLeft();
                    int bobId = generateClientId();
                    message.getClient().setClientId(bobId);

                    Client alice = clients.get(roomId).getLeft();

                    rooms.put(roomId, Pair.of(aliceId, bobId));
                    clients.put(roomId, Pair.of(alice, message.getClient()));
                    outputTopics.put(roomId + "_" + aliceId, getOutputTopicName(roomId, bobId));
                    outputTopics.put(roomId + "_" + bobId, getOutputTopicName(roomId, bobId));

                    log.info("{} output topic {}", aliceId, getOutputTopicName(roomId, bobId));
                    log.info("{} output topic {}", bobId, getOutputTopicName(roomId, bobId));

                    startRoom(roomId, alice, message.getClient());
                } else {
                    int aliceId = generateClientId();
                    message.getClient().setClientId(aliceId);
                    rooms.put(roomId, Pair.of(aliceId, null));
                    clients.put(roomId, Pair.of(message.getClient(), null));
                    log.info("Connection is created");
                }
            }

            if (message.getAction().equals("send")) {
                String outputTopic = outputTopics.get(message.getRoomId() + "_" + message.getClientId());
                log.info(outputTopic);
                kafkaWriter.processing(message.toBytes(), outputTopic);
            }
        } catch (Exception ex) {
            log.error("Error while working with client");
            log.info(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }
    }

    private static String getOutputTopicName(int roomId, int clientId) {
        return TOPIC_PREFIX + roomId + "_" + clientId;
    }

    private static void startRoom(int roomId, Client alice, Client bob) {
        BigInteger[] parameters = DiffieHellman.generateParameters(300);
        KafkaReader kafkaReaderAlice = new KafkaReaderImpl(alice, roomId, parameters);
        KafkaReader kafkaReaderBob = new KafkaReaderImpl(bob, roomId, parameters);

        service.submit(kafkaReaderAlice::processing);
        service.submit(kafkaReaderBob::processing);

        CipherInfoMessage cipherMessageAlice = CipherInfoMessage.builder()
                .typeMessage("cipherInfo")
                .nameAlgorithm("RC5")
                .namePadding("ANSIX923")
                .encryptionMode("ECB")
                .sizeKeyIbBits(64)
                .sizeBlockInBits(64)
                .publicKey(generatePublicKey(
                        parameters[1],
                        generatePrivateKey(),
                        parameters[0]
                ).toByteArray())
                .build();

        CipherInfoMessage cipherMessageBob = CipherInfoMessage.builder()
                .typeMessage("cipherInfo")
                .nameAlgorithm("RC5")
                .namePadding("ANSIX923")
                .encryptionMode("ECB")
                .sizeKeyIbBits(64)
                .sizeBlockInBits(64)
                .publicKey(generatePublicKey(
                        parameters[1],
                        generatePrivateKey(),
                        parameters[0]
                ).toByteArray())
                .build();

        kafkaWriter.processing(cipherMessageAlice.toBytes(), getOutputTopicName(roomId, alice.getClientId()));
        kafkaWriter.processing(cipherMessageBob.toBytes(), getOutputTopicName(roomId, bob.getClientId()));

        alice.sendMessageToClient(roomId, "text", "hello bob".getBytes());
        bob.sendMessageToClient(roomId, "text", "hello alice".getBytes());
    }

    private static int generateClientId() {
        return random.nextInt(Integer.MAX_VALUE);
    }

    private static BigInteger generatePrivateKey() {
        return new BigInteger(100, new Random());
    }

    private static BigInteger generatePublicKey(BigInteger g, BigInteger privateKey, BigInteger p) {
        return g.modPow(privateKey, p);
    }
}
