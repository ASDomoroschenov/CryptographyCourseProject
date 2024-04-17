package ru.mai.cipher.cipher_interface;

import java.util.concurrent.ExecutionException;

public interface CipherMode {
    byte[] encrypt(byte[] text) throws IllegalArgumentException, ExecutionException, InterruptedException;

    byte[] decrypt(byte[] text) throws IllegalArgumentException, ExecutionException, InterruptedException;
}
