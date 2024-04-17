package ru.mai.cipher.cipher_thread.file.file_interface;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface FileThreadTaskCipher {
    byte[] apply(String pathToInputFile, long skipValue, long sizePartBytesThread, FileThreadCipher.CipherAction action) throws IOException, ExecutionException, InterruptedException;
}
