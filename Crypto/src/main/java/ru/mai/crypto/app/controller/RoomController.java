package ru.mai.crypto.app.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.network.Mode;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.mai.crypto.app.room.impl.user.User;
import ru.mai.crypto.app.room.impl.user.UserFactory;
import ru.mai.crypto.app.room.model.Message;
import ru.mai.crypto.diffie_hellman.DiffieHellman;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Controller
@CrossOrigin
@RequestMapping("/room")
@SessionAttributes("user")
public class RoomController {
    @Autowired
    private SimpMessagingTemplate template;
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Map<Integer, BigInteger[]> roomParameters = new HashMap<>();
    private static final Map<Integer, Pair<User, User>> users = new HashMap<>();
    private static final Map<Integer, Pair<List<String>, List<String>>> messages = new HashMap<>();

    @GetMapping("/{roomId}")
    public String roomProcessing(@PathVariable String roomId) {
        log.info("You are in room {}", roomId);
        int id = Integer.parseInt(roomId);

        if (roomParameters.containsKey(id)) {
            log.info("Create Bob...");

            BigInteger[] parameters = roomParameters.get(id);
            User alice = users.get(id).getLeft();
            User bob = UserFactory.createBobUser(id, parameters);
            bob.setUrlUser("http://localhost:8080/room/" + roomId + "/bob");

            users.put(id, Pair.of(alice, bob));
            messages.put(id, Pair.of(new ArrayList<>(), new ArrayList<>()));

            log.info("Success create Bob");

            startRoom(alice, bob);

            return "redirect:/room/" + roomId + "/bob";
        } else {
            log.info("Create Alice...");

            BigInteger[] parameters = DiffieHellman.generateParameters(300);
            User alice = UserFactory.createAliceUser(id, parameters);
            alice.setUrlUser("http://localhost:8080/room/" + roomId + "/alice");

            roomParameters.put(id, parameters);
            users.put(id, Pair.of(alice, null));
            messages.put(id, Pair.of(new ArrayList<>(), null));

            log.info("Success create Alice");

            return "redirect:/room/" + roomId + "/alice";
        }
    }

    @GetMapping("/{roomId}/alice")
    public String alice(@PathVariable String roomId) {
        return "alice";
    }

    @ResponseBody
    @PostMapping("/{roomId}/alice")
    public ResponseEntity<String> sendMessageAlice(@PathVariable String roomId, @RequestParam String message) {
        // Обработка полученного сообщения
        int id = Integer.parseInt(roomId);
        User alice = users.get(id).getLeft();
        alice.sendMessage(new Message("message", "text", message.getBytes()));
        return ResponseEntity.ok("Message sent successfully"); // Возврат ответа клиенту
    }

    @GetMapping("/{roomId}/alice/show")
    public String showMessageAlice(@PathVariable String roomId, @RequestParam String message) {
        log.info("Triggering WebSocket update for room: {}", roomId);
        log.info(message);
        template.convertAndSend("/topic/room/" + roomId + "/alice", message);
        messages.get(Integer.parseInt(roomId)).getLeft().add(message);
        return "alice";
    }


    @GetMapping("/{roomId}/bob")
    public String bob(@PathVariable String roomId) {
        return "bob";
    }

    @ResponseBody
    @PostMapping("/{roomId}/bob")
    public ResponseEntity<String> sendMessageBob(@PathVariable String roomId, @RequestParam String message) {
        int id = Integer.parseInt(roomId);
        User bob = users.get(id).getRight();
        bob.sendMessage(new Message("message", "text", message.getBytes()));
        return ResponseEntity.ok("Message sent successfully");
    }

    @GetMapping("/{roomId}/bob/show")
    public String showMessageBob(@PathVariable String roomId, @RequestParam String message) {
        log.info("Triggering WebSocket update for room: {}", roomId);
        log.info(message);
        template.convertAndSend("/topic/room/" + roomId + "/bob", message);
        return "bob";
    }

    private void startRoom(User alice, User bob) {
        service.submit(alice::processing);
        service.submit(bob::processing);

        alice.sendCipherInfo();
        bob.sendCipherInfo();
    }
}