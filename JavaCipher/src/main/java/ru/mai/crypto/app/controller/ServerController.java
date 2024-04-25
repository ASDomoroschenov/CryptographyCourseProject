package ru.mai.crypto.app.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@RestController
public class ServerController {
    private static final Random random = new Random();
    private static final Map<Integer, Integer> rooms = new HashMap<>();

    @GetMapping("/connect/{roomId}")
    public RedirectView connect(@PathVariable String roomId) {
        log.info("Try to connect {} room...", roomId);
        Integer room = Integer.parseInt(roomId);

        if (rooms.containsKey(room) && rooms.get(room) < 2) {
            log.info("Successful connection");
            rooms.put(room, rooms.get(room) + 1);
            return new RedirectView("/room/" + roomId);
        } else {
            log.error("Error to connect room {}", roomId);
            throw new IllegalArgumentException("Error connecting to room " + roomId);
        }
    }

    @GetMapping("/create_room")
    public void createRoom() {
        log.info("Try to create room...");
        int roomId;

        do {
            roomId = generateRoomId();
        } while (rooms.containsKey(roomId));

        rooms.put(roomId, 0);

        log.info("Room {} is created", roomId);
    }

    @GetMapping("/disconnect/{roomId}")
    public void closeRoom(@PathVariable String roomId) {
        log.info("Try to disconnect room {}...", roomId);
    }

    private int generateRoomId() {
        return random.nextInt(Integer.MAX_VALUE);
    }
}
