package ru.mai.crypto.app.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

@Data
@Builder
public class ClientRoomInfo {
    private String nameInputTopic;
    private String nameOutputTopic;
    private BigInteger[] encryptionParam;
    private BigInteger privateKey;
    private BigInteger publicKey;
}
