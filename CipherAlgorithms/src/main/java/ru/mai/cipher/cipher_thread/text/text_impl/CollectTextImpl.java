package ru.mai.cipher.cipher_thread.text.text_impl;

import org.apache.commons.lang3.tuple.Pair;
import ru.mai.cipher.cipher_thread.text.text_interface.CollectText;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CollectTextImpl implements CollectText {
    @Override
    public byte[] collect(List<Future<Pair<Integer, byte[]>>> futures, int textLength) throws ExecutionException, InterruptedException {
        byte[] result = new byte[textLength];

        while (!futures.isEmpty()) {
            List<Future<Pair<Integer, byte[]>>> listNotDoneFuture = futures.stream().filter(item -> !item.isDone()).toList();

            for (Future<Pair<Integer, byte[]>> future : futures) {
                if (!listNotDoneFuture.contains(future)) {
                    Pair<Integer, byte[]> pair = future.get();
                    System.arraycopy(pair.getRight(), 0, result, pair.getLeft(), pair.getRight().length);
                }
            }

            futures = listNotDoneFuture;
        }

        return result;
    }
}
