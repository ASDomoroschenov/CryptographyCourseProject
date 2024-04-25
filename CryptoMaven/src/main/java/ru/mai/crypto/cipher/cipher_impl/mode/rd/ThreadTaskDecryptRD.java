package ru.mai.crypto.cipher.cipher_impl.mode.rd;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.crypto.cipher.cipher_interface.CipherService;
import ru.mai.crypto.cipher.cipher_thread.text.text_interface.TextThreadTask;
import ru.mai.crypto.cipher.utils.BitsUtil;

@AllArgsConstructor
public class ThreadTaskDecryptRD implements TextThreadTask {
    private final CipherService cipherService;
    private final byte[][] counterBlocks;

    public ThreadTaskDecryptRD(CipherService cipherService, byte[] text, byte[] initialVector) {
        this.cipherService = cipherService;
        counterBlocks = new byte[text.length / cipherService.getBlockSizeInBytes()][cipherService.getBlockSizeInBytes()];
        byte[] delta = BitsUtil.splitInHalf(initialVector).getRight();
        byte[] counter = cipherService.encryptBlock(initialVector);

        for (int i = 0; i < text.length / cipherService.getBlockSizeInBytes(); i++) {
            counterBlocks[i] = counter;
            counter = getNextCounter(counter, delta);
        }
    }

    private byte[] getNextCounter(byte[] counter, byte[] delta) {
        byte[] result = new byte[counter.length];
        byte remind = 0;

        for (int i = 0; i < delta.length; i++) {
            result[i] = (byte) ((counter[counter.length - i - 1] + delta[delta.length - i - 1] + remind) % 256);
            remind = (byte) ((counter[counter.length - i - 1] + delta[delta.length - i - 1]) / 256);
        }

        if (remind != 0) {
            result[result.length - delta.length - 1] = (byte) ((counter[counter.length - delta.length - 1] + remind) % 256);
        }

        return result;
    }

    @Override
    public Pair<Integer, byte[]> apply(byte[] text, int indexBegin, int textBlockSize, int countBlocks) {
        byte[] result = new byte[countBlocks * textBlockSize];
        byte[] textBlock = new byte[textBlockSize];

        for (int i = 0; i < countBlocks; i++) {
            System.arraycopy(text, indexBegin + i * textBlockSize, textBlock, 0, textBlockSize);
            byte[] cipherBlockText = BitsUtil.xor(cipherService.decryptBlock(textBlock), counterBlocks[(indexBegin + i * textBlockSize) / textBlockSize]);
            System.arraycopy(cipherBlockText, 0, result, i * textBlockSize, textBlockSize);
        }

        return Pair.of(indexBegin, result);
    }
}