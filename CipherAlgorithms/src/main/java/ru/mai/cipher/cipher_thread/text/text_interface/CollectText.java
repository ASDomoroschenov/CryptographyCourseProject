package ru.mai.cipher.cipher_thread.text.text_interface;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface CollectText {
    byte[] collect(List<Future<Pair<Integer, byte[]>>> futures, int textLength) throws ExecutionException, InterruptedException;
}
