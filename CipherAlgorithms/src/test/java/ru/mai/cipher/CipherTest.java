package ru.mai.cipher;

import org.junit.jupiter.api.Test;
import ru.mai.cipher.cipher_impl.RC5;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CipherTest {
    private static final Cipher.EncryptionMode[] MODES = {
            Cipher.EncryptionMode.CBC,
            Cipher.EncryptionMode.CFB,
            Cipher.EncryptionMode.CTR,
            Cipher.EncryptionMode.ECB,
            Cipher.EncryptionMode.OFB,
            Cipher.EncryptionMode.PCBC,
            Cipher.EncryptionMode.RD
    };
    private static final String[] FILES = {
            "/home/alexandr/CryptographyCourseProject/CipherAlgorithms/src/main/resources/text1.txt",
            "/home/alexandr/CryptographyCourseProject/CipherAlgorithms/src/main/resources/text2.txt"
    };


    @Test
    void testRC5Service() throws IOException {
        byte[] key = {(byte) 0b11111111, (byte) 0b01010111, (byte) 0b11111111, (byte) 0b01010111};
        byte[][] initializationVectors = {
                {0b01110111, 0b01010111, 0b01010111, 0b01010111},
                {0b01110111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111},
                {0b01110111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111}
        };
        int[] sizesBlock = {32, 64, 128};

        for (String pathToFile : FILES) {
            for (int sizeBlock : sizesBlock) {
                RC5 rc5 = new RC5(3, sizeBlock, 16, key);
                for (Cipher.EncryptionMode encryptionMode : MODES) {
                    for (byte[] initializationVector : initializationVectors) {
                        if (initializationVector.length == sizeBlock / Byte.SIZE) {
                            Cipher cipher = new Cipher(initializationVector, rc5, Cipher.PaddingMode.ANSIX923, encryptionMode);

                            String encryptedFile = cipher.encryptFile(pathToFile);
                            String decryptedFile = cipher.decryptFile(encryptedFile);
                            Path pathDecryptedFile = Path.of(decryptedFile);

                            assertArrayEquals(
                                    Files.readAllBytes(Path.of(pathToFile)),
                                    Files.readAllBytes(pathDecryptedFile)
                            );

                            Files.delete(Path.of(encryptedFile));
                            Files.delete(pathDecryptedFile);
                        }
                    }
                }
            }
        }
    }
}