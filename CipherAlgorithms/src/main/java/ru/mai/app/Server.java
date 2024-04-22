package ru.mai.app;

import com.typesafe.config.Config;
import ru.mai.app.app_impl.ConfigReaderImpl;
import ru.mai.app.app_impl.KafkaConfigBuilderImpl;
import ru.mai.app.app_interface.KafkaConfigBuilder;

public class Server {
    private static long countRooms = 38;
    private static final Config appConfig = new ConfigReaderImpl().loadConfig();
    private static final KafkaConfigBuilder configBuilder = new KafkaConfigBuilderImpl();

    private Server() {}

    public static void startRoom() {
        Config configAlice = configBuilder.build(
                "input_alice_" + countRooms,
                "input_bob_" + countRooms,
                "alice_group_" + countRooms,
                "earliest"
        );
        Config configBob = configBuilder.build(
                "input_bob_" + countRooms,
                "input_alice_" + countRooms,
                "bob_group_" + countRooms,
                "earliest"
        );

        Room room = new Room(1, 2, appConfig, configAlice, configBob);
        room.start();
    }
}
