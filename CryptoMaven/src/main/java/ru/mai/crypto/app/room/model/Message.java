package ru.mai.crypto.app.room.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mai.crypto.app.Client;

import java.io.Serializable;

@Data
@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message implements Serializable {
    private static final ObjectMapper mapper = new ObjectMapper();
    private Client client;
    private int clientId;
    private int roomId;
    private String action;
    private String format;
    private byte[] bytes;

    public byte[] toBytes() {
        try {
            return mapper.writeValueAsString(this).getBytes();
        } catch (JsonProcessingException ex) {
            log.error("Error while processing message to json bytes");
        }

        return new byte[0];
    }
}