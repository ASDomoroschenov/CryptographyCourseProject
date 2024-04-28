package ru.mai.crypto.room.kafka.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ru.mai.crypto.room.kafka.ConfigReader;

public class ConfigReaderImpl implements ConfigReader {
    @Override
    public Config loadConfig() {
        return ConfigFactory.parseResources("application.conf");
    }
}
