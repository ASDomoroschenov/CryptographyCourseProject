package ru.mai;

import lombok.extern.slf4j.Slf4j;
import ru.mai.cipher.Cipher;
import ru.mai.cipher.cipher_impl.RC5;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
public class Main {
    public static void main(String[] args) throws IOException {
        byte[] key = {(byte) 0b11111111, (byte) 0b01010111, (byte) 0b11111111, (byte) 0b01010111};
        Cipher.EncryptionMode[] mode = {
                Cipher.EncryptionMode.CBC,
                Cipher.EncryptionMode.CFB,
                Cipher.EncryptionMode.CTR,
                Cipher.EncryptionMode.ECB,
                Cipher.EncryptionMode.OFB,
                Cipher.EncryptionMode.PCBC,
                Cipher.EncryptionMode.RD
        };
        int[] sizesBlock= {32, 64, 128};
        byte[][] initializationVectors = {
                {0b01110111, 0b01010111, 0b01010111, 0b01010111},
                {0b01110111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111},
                {0b01110111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111}
        };

        String pathToFile = "/home/alexandr/CryptographyCourseProject/CipherAlgorithms/src/main/resources/text1.txt";

        for (int j = 0; j < sizesBlock.length; j++) {
            RC5 rc5 = new RC5(3, sizesBlock[j], 16, key);

            for (int i = 0; i < mode.length; i++) {
                Cipher cipher = new Cipher(initializationVectors[j], rc5, Cipher.PaddingMode.ANSIX923, mode[i]);

                String encryptedFile = cipher.encryptFile(pathToFile);
                String decryptedFile = cipher.decryptFile(encryptedFile);

                log.info(String.valueOf(Arrays.equals(
                        Files.readAllBytes(Path.of(pathToFile)),
                        Files.readAllBytes(Path.of(decryptedFile))
                )));

                Files.delete(Path.of(encryptedFile));
                Files.delete(Path.of(decryptedFile));
            }
        }
    }
}