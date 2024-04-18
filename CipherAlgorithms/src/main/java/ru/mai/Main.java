package ru.mai;

import lombok.extern.slf4j.Slf4j;
import ru.mai.cipher.cipher_impl.LOKI97;
import ru.mai.cipher.utils.BitsUtil;

import java.util.Arrays;
import java.util.Random;

@Slf4j
public class Main {
    public static void main(String[] args) {
        byte[] key = {
                (byte) 0b01011101,
                (byte) 0b01111000,
                (byte) 0b01011100,
                (byte) 0b00010110,
                (byte) 0b01001011,
                (byte) 0b00101010,
                (byte) 0b01110111,
                (byte) 0b00110011,
                (byte) 0b10010111,
                (byte) 0b00010000,
                (byte) 0b01111010,
                (byte) 0b00001001,
                (byte) 0b00111110,
                (byte) 0b10010101,
                (byte) 0b10000011,
                (byte) 0b11000011
        };
        byte[] text = {
                (byte) 0b01011101,
                (byte) 0b01111000,
                (byte) 0b01011100,
                (byte) 0b00010110,
                (byte) 0b01001011,
                (byte) 0b00101010,
                (byte) 0b01110111,
                (byte) 0b00110011,
                (byte) 0b10010111,
                (byte) 0b00010000,
                (byte) 0b01111010,
                (byte) 0b00001001,
                (byte) 0b00111110,
                (byte) 0b10010101,
                (byte) 0b10000011,
                (byte) 0b11000011
        };

        System.out.println(Arrays.toString(text));
        LOKI97 loki97 = new LOKI97(key, 128);

        byte[] encryptedText = loki97.encryptBlock(text);
        byte[] decryptedText = loki97.decryptBlock(encryptedText);

        System.out.println(Arrays.toString(decryptedText));
    }
}

//0000000000000000011100000100100010000110000110110000111100111111
//0000000000000000000000000000000010000110000110110000111100111111