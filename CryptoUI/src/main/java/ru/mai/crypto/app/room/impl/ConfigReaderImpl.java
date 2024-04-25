package ru.mai.crypto.app.room.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ru.mai.crypto.app.room.ConfigReader;

public class ConfigReaderImpl implements ConfigReader {
    @Override
    public Config loadConfig() {
        return ConfigFactory.parseResources("application.conf");
    }
}
