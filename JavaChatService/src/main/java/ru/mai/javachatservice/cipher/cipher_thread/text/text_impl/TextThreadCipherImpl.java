package ru.mai.javachatservice.cipher.cipher_thread.text.text_impl;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.javachatservice.cipher.cipher_thread.text.text_interface.CollectText;
import ru.mai.javachatservice.cipher.cipher_thread.text.text_interface.TextThreadCipher;
import ru.mai.javachatservice.cipher.cipher_thread.text.text_interface.TextThreadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@AllArgsConstructor
public class TextThreadCipherImpl implements TextThreadCipher {
    private int blockSize;
    private TextThreadTask threadTask;
    private CollectText collectText;

    @Override
    public byte[] cipher(byte[] text) throws ExecutionException, InterruptedException {
        if (text == null) {
            throw new IllegalArgumentException("Illegal bytes text");
        }

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(availableProcessors);
        List<Future<Pair<Integer, byte[]>>> futures = new ArrayList<>();
        int countBlocks = ((text.length / blockSize) + availableProcessors - 1) / availableProcessors;

        for (int i = 0; i < text.length; i += blockSize * countBlocks) {
            int finalI = i;
            int finalCountBlocks = i + blockSize * countBlocks < text.length ? countBlocks : (text.length - i) / blockSize;
            futures.add(service.submit(() -> threadTask.apply(text, finalI, blockSize, finalCountBlocks)));
        }

        byte[] result;

        try {
            result = collectText.collect(futures, text.length);
        } catch (InterruptedException ex) {
            throw new InterruptedException(ex.getMessage());
        } catch (ExecutionException ex) {
            throw new ExecutionException(ex);
        } finally {
            service.shutdown();

            try {
                if (!service.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    service.shutdownNow();
                }
            } catch (InterruptedException e) {
                service.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        return result;
    }
}