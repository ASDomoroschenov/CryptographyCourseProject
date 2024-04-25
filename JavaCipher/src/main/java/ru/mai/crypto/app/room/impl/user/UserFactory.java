package ru.mai.crypto.app.room.impl.user;

import com.typesafe.config.Config;
import ru.mai.crypto.app.room.KafkaConfigBuilder;
import ru.mai.crypto.app.room.impl.ConfigReaderImpl;
import ru.mai.crypto.app.room.impl.KafkaConfigBuilderImpl;

import java.math.BigInteger;

public class UserFactory {
    private static final Config appConfig = new ConfigReaderImpl().loadConfig();
    private static final KafkaConfigBuilder configBuilder = new KafkaConfigBuilderImpl();

    private UserFactory() {}

    public static User createAliceUser(long roomId, BigInteger[] parameters) {
        return new User(appConfig, createConfigAlice(configBuilder, roomId), parameters);
    }

    public static User createBobUser(long roomId, BigInteger[] parameters) {
        return new User(appConfig, createConfigBob(configBuilder, roomId), parameters);
    }

    public static Config createConfigAlice(KafkaConfigBuilder configBuilder, long roomId) {
        return configBuilder.build(
                "input_alice_" + roomId,
                "input_bob_" + roomId,
                "alice_group_" + roomId,
                "earliest"
        );
    }

    public static Config createConfigBob(KafkaConfigBuilder configBuilder, long roomId) {
        return configBuilder.build(
                "input_bob_" + roomId,
                "input_alice_" + roomId,
                "bob_group_" + roomId,
                "earliest"
        );
    }
}
