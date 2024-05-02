package ru.mai.javachatservice.kafka;

import com.typesafe.config.Config;

public interface ConfigReader {
    Config loadConfig();
}