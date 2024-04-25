module ru.mai.crypto {
    requires javafx.controls;
    requires javafx.fxml;
    requires static commons.lang3;
    requires static org.apache.commons.io;
    requires org.slf4j;
    requires static lombok;
    requires static com.fasterxml.jackson.core;
    requires static com.fasterxml.jackson.databind;
    requires static typesafe.config;
    requires static kafka.clients;

    opens ru.mai.crypto to javafx.fxml;
    exports ru.mai.crypto;
}