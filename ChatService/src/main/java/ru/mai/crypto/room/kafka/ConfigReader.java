package ru.mai.crypto.room.kafka;

import com.typesafe.config.Config;

public interface ConfigReader {
    Config loadConfig();
}