package ru.mai.app;

import com.typesafe.config.Config;
import ru.mai.app.app_impl.user.Alice;
import ru.mai.app.app_impl.user.Bob;
import ru.mai.app.model.Message;

public class Room {
    private static final byte[] key = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final byte[] initializationVector = {1, 2, 3, 4, 5, 6, 7, 8};
    private final long aliceId;
    private final long bobId;
    private final Config appConfig;
    private final Config configAlice;
    private final Config configBob;

    public Room(long aliceId, long bobId, Config appConfig, Config configAlice, Config configBob) {
        this.aliceId = aliceId;
        this.bobId = bobId;
        this.appConfig = appConfig;
        this.configAlice = configAlice;
        this.configBob = configBob;
    }

    public void start() {
        Alice alice = new Alice(aliceId, appConfig, configAlice, key, initializationVector);
        Bob bob = new Bob(bobId, appConfig, configBob, key, initializationVector);

        alice.sendMessage(new Message("message", "text", "Hello bob!".getBytes()));
        bob.sendMessage(new Message("message", "text", "Hello Alice!".getBytes()));

        bob.processing();
        alice.processing();
    }
}
