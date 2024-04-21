package ru.mai.app.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Data
@Slf4j
@AllArgsConstructor
public class Message {
    private static final ObjectMapper mapper = new ObjectMapper();
    private String typeMessage;
    private String typeFormat;
    @JsonSerialize(using = ByteArrayToIntArraySerializer.class)
    private byte[] bytes;

    public byte[] toBytes() {
        try {
            return mapper.writeValueAsString(this).getBytes();
        } catch (JsonProcessingException ex) {
            log.error("Error while processing message to json bytes");
        }

        return new byte[0];
    }

    static class ByteArrayToIntArraySerializer extends JsonSerializer<byte[]> {
        @Override
        public void serialize(byte[] bytes, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartArray();

            for (byte byteItem : bytes) {
                jsonGenerator.writeNumber(byteItem);
            }

            jsonGenerator.writeEndArray();
        }
    }
}