package ru.mai.app.app_impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ru.mai.app.app_interface.ConfigReader;

public class ConfigReaderImpl implements ConfigReader {
    @Override
    public Config loadConfig() {
        return ConfigFactory.parseResources("application.conf");
    }
}
