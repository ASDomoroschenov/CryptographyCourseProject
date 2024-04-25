package ru.mai.crypto.cipher.cipher_thread.text.text_interface;

import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.ExecutionException;

public interface TextThreadTask {
    Pair<Integer, byte[]> apply(byte[] text, int indexBegin, int textBlockSize, int countBlocks) throws ExecutionException, InterruptedException;
}
