package ru.mai.cipher.cipher_thread.text.text_interface;

import java.util.concurrent.ExecutionException;

public interface TextThreadCipher {
    byte[] cipher(byte[] text) throws ExecutionException, InterruptedException;
}
