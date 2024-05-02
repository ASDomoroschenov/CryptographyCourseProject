package ru.mai.javachatservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@Slf4j
@SpringBootApplication
public class JavaChatServiceApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(JavaChatServiceApplication.class, args);
//        ChatServer server = context.getBean(ChatServer.class);
//        Client client = server.authorization("Danya", "RC5");
//        Room room = server.connectToRoom(client, 1);
//
//        log.info(client.toString());
//        log.info(room.toString());
//
//        boolean isDisconnect = server.disconnectFromRoom(client, room.getId());
//
//        log.info("Disconnect {}", isDisconnect);
    }
}
