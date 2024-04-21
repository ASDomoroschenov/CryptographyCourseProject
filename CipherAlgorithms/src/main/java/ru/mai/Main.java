package ru.mai;

import lombok.extern.slf4j.Slf4j;
import ru.mai.app.Server;

@Slf4j
public class Main {
    public static void main(String[] args) {
        Server.startRoom();
    }
}