package ru.mai.app;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import ru.mai.app.app_impl.user.User;
import ru.mai.app.model.Message;
import ru.mai.diffie_hellman.DiffieHellman;

import java.math.BigInteger;

@Slf4j
public class Room {
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
        BigInteger[] parameters = DiffieHellman.generateParameters(300);
        User alice = new User(aliceId, appConfig, configAlice, parameters);
        User bob = new User(bobId, appConfig, configBob, parameters);

        alice.sendMessage(new Message("message", "text", "Hello bob!".getBytes()));
        bob.sendMessage(new Message("message", "text", "Hello Alice!".getBytes()));

        log.info("Alice id = {}", alice.getUserId());
        log.info("Bob id = {}", bob.getUserId());

        bob.processing();
        alice.processing();
    }
}
