package ru.mai.javachatservice.model.messages.json_parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ru.mai.javachatservice.cipher.Cipher;
import ru.mai.javachatservice.cipher.cipher_impl.LOKI97;
import ru.mai.javachatservice.cipher.cipher_impl.RC5;
import ru.mai.javachatservice.cipher.cipher_interface.CipherService;
import ru.mai.javachatservice.model.messages.CipherInfoMessage;

import java.math.BigInteger;
import java.util.Arrays;

@Slf4j
public class CipherInfoMessageParser {
    private static final String UNEXPECTED_VALUE = "Unexpected value: ";

    private CipherInfoMessageParser() {
    }

    public static Cipher getCipher(CipherInfoMessage cipherInfo, BigInteger privateKey, BigInteger modulo) {
        byte[] key = getKey(cipherInfo.getPublicKey(), cipherInfo.getSizeKeyInBits(), privateKey, modulo);
        byte[] initializationVector = cipherInfo.getInitializationVector();

        log.info(Arrays.toString(key));

        CipherService cipherService = getCipherService(
                cipherInfo.getNameAlgorithm(),
                key,
                cipherInfo.getSizeKeyInBits(),
                cipherInfo.getSizeBlockInBits()
        );

        return new Cipher(
                initializationVector,
                cipherService,
                getPadding(cipherInfo.getNamePadding()),
                getEncryptionMode(cipherInfo.getEncryptionMode())
        );
    }

    public static byte[] getKey(byte[] publicKey, int sizeKeyInBits, BigInteger privateKey, BigInteger modulo) {
        BigInteger publicKeyNumber = new BigInteger(publicKey);
        BigInteger key = publicKeyNumber.modPow(privateKey, modulo);
        byte[] keyBytes = key.toByteArray();
        byte[] result = new byte[sizeKeyInBits / Byte.SIZE];
        System.arraycopy(keyBytes, 0, result, 0, sizeKeyInBits / Byte.SIZE);
        return result;
    }

    public static CipherService getCipherService(String nameAlgorithm, byte[] key, int sizeKeyInBits, int sizeBlockInBits) {
        return switch (nameAlgorithm) {
            case "LOKI97" -> new LOKI97(key, sizeKeyInBits);
            case "RC5" -> new RC5(sizeKeyInBits, sizeBlockInBits, 16, key);
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + nameAlgorithm);
        };
    }

    public static Cipher.PaddingMode getPadding(String namePadding) {
        return switch (namePadding) {
            case "ANSIX923" -> Cipher.PaddingMode.ANSIX923;
            case "ISO10126" -> Cipher.PaddingMode.ISO10126;
            case "PKCS7" -> Cipher.PaddingMode.PKCS7;
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + namePadding);
        };
    }

    public static Cipher.EncryptionMode getEncryptionMode(String encryptionMode) {
        return switch (encryptionMode) {
            case "CBC" -> Cipher.EncryptionMode.CBC;
            case "CFB" -> Cipher.EncryptionMode.CFB;
            case "CTR" -> Cipher.EncryptionMode.CTR;
            case "ECB" -> Cipher.EncryptionMode.ECB;
            case "OFB" -> Cipher.EncryptionMode.OFB;
            case "PCBC" -> Cipher.EncryptionMode.PCBC;
            case "RD" -> Cipher.EncryptionMode.RD;
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + encryptionMode);
        };
    }
}
