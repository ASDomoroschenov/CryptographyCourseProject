module ru.mai.crypto {
    requires javafx.controls;
    requires javafx.fxml;

    // apache commons
    requires org.apache.commons.lang3;
    requires org.apache.commons.io;

    // code generator
    requires static org.slf4j;
    requires static ch.qos.logback.classic;
    requires static lombok;

    // json
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    // kafka
    requires kafka.clients;

    // config
    requires typesafe.config;

    exports ru.mai.crypto.server to javafx.graphics, javafx.fxml;
    exports ru.mai.crypto.client to javafx.graphics, javafx.fxml;
    exports ru.mai.crypto.app.room.model to com.fasterxml.jackson.databind;
    exports ru.mai.crypto.app to com.fasterxml.jackson.databind;
}