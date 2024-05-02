package ru.mai.javachatservice.cipher.cipher_thread.file.file_interface;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface FileThreadCipher {
    enum CipherAction {
        ENCRYPT,
        DECRYPT
    }

    String cipher(String pathToInputFile, String pathToOutputFile, CipherAction action) throws IOException, ExecutionException, InterruptedException;
}
