package ru.mai.javachatservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import ru.mai.javachatservice.model.Client;
import ru.mai.javachatservice.repository.CipherInfoRepository;
import ru.mai.javachatservice.repository.ClientRepository;
import ru.mai.javachatservice.repository.RoomRepository;
import ru.mai.javachatservice.server.ChatServer;

@Slf4j
@SpringBootApplication
public class JavaChatServiceApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(JavaChatServiceApplication.class, args);
        ChatServer server = context.getBean(ChatServer.class);
        Client client = server.authorization("Danya", "RC5");
        log.info("Authorization with id {}", client.getId());
    }

}
