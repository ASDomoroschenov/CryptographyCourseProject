package ru.mai.crypto.room.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private static final ObjectMapper mapper = new ObjectMapper();
    private String typeMessage;
    private String typeFormat;
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