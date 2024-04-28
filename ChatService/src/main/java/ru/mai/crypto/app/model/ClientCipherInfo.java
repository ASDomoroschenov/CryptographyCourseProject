package ru.mai.crypto.app.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientCipherInfo {
    private String nameAlgorithm;
    private String namePadding;
    private String encryptionMode;
    private int sizeKeyInBits;
    private int sizeBlockInBits;
    private byte[] initializationVector;
}
