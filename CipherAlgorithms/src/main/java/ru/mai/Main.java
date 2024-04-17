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
        byte[] initializationVector = {0b01110111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111, 0b01010111};
        RC5 rc5 = new RC5(32, 128, 16, key);
        Cipher cipher = new Cipher(initializationVector, rc5, Cipher.PaddingMode.ANSIX923, Cipher.EncryptionMode.RD);

        String pathToFile = "/home/alexandr/CryptographyCourseProject/CipherAlgorithms/src/main/resources/text.txt";
        String encryptedFile = cipher.encryptFile(pathToFile);
        String decryptedFile = cipher.decryptFile(encryptedFile);

        log.info(String.valueOf(Arrays.equals(
                Files.readAllBytes(Path.of(pathToFile)),
                Files.readAllBytes(Path.of(decryptedFile))
        )));
    }
}