package ru.mai.crypto.app.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.mai.crypto.app.room.impl.user.User;
import ru.mai.crypto.app.room.impl.user.UserFactory;
import ru.mai.crypto.app.room.model.Message;
import ru.mai.crypto.diffie_hellman.DiffieHellman;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Controller
@RequestMapping("/room")
public class RoomController {
    private static final String DEFAULT_TYPE_MESSAGE = "message";
    private static final String DEFAULT_TYPE_FORMAT = "text";
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Map<Integer, BigInteger[]> roomParameters = new HashMap<>();
    private static final Map<Integer, Pair<User, User>> users = new HashMap<>();

    @GetMapping("/{roomId}")
    public ModelAndView roomProcessing(@PathVariable String roomId) {
        log.info("You are in room {}", roomId);
        int id = Integer.parseInt(roomId);

        if (roomParameters.containsKey(id)) {
            log.info("Create Bob...");

            BigInteger[] parameters = roomParameters.get(id);
            User alice = users.get(id).getLeft();
            User bob = UserFactory.createBobUser(id, parameters);
            bob.setUrlUser("http://localhost:8080/room/" + roomId + "/bob");

            users.put(id, Pair.of(alice, bob));

            log.info("Success create Bob");

            startRoom(alice, bob);

            return new ModelAndView("redirect:/room/" + roomId + "/bob");
        } else {
            log.info("Create Alice...");

            BigInteger[] parameters = DiffieHellman.generateParameters(300);
            User alice = UserFactory.createAliceUser(id, parameters);
            alice.setUrlUser("http://localhost:8080/room/" + roomId + "/alice");

            roomParameters.put(id, parameters);
            users.put(id, Pair.of(alice, null));

            log.info("Success create Alice");

            return new ModelAndView("redirect:/room/" + roomId + "/alice");
        }
    }


    @GetMapping("/{roomId}/alice")
    public String Alice(@PathVariable String roomId) {
        log.info("Alice in room {}", roomId);
        return "user";
    }

    @PostMapping("/{roomId}/alice")
    public String sendMessageAlice(@PathVariable String roomId, @RequestParam String message) {
        int id = Integer.parseInt(roomId);
        User alice = users.get(id).getLeft();

        log.info("Alice trying to send message '{}'", message);
        log.info(alice.getUrlUser());
        alice.sendMessage(new Message(DEFAULT_TYPE_MESSAGE, DEFAULT_TYPE_FORMAT, message.getBytes()));

        return "user";
    }

    @GetMapping("/{roomId}/bob")
    public String Bob(@PathVariable String roomId) {
        log.info("Bob in room {}", roomId);
        return "user";
    }

    @PostMapping("/{roomId}/bob")
    public String sendMessageBob(@PathVariable String roomId) {
        int id = Integer.parseInt(roomId);
        User bob = users.get(id).getRight();
        log.info("Bob trying to send message");
        return "user";
    }

    @PostMapping("/{roomId}/bob/show")
    public RedirectView showMessageBob(@PathVariable String roomId, @RequestBody Message message, Model model) {
        int id = Integer.parseInt(roomId);
        User bob = users.get(id).getRight();
        log.info("Bob is requesting to show the message {}", new String(message.getBytes()));
        model.addAttribute("receivedMessage", new String(message.getBytes()));
        log.info(bob.getUrlUser());
        return new RedirectView(bob.getUrlUser());
    }

    private void startRoom(User alice, User bob) {
        service.submit(alice::processing);
        service.submit(bob::processing);

        alice.sendCipherInfo();
        bob.sendCipherInfo();
    }
}