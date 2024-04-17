package ru.mai.cipher.cipher_impl.mode.ctr;

import org.apache.commons.lang3.tuple.Pair;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.cipher_thread.text.text_interface.TextThreadTask;
import ru.mai.cipher.utils.BytesUtil;

public class ThreadTaskCipherCTR implements TextThreadTask {
    private final CipherService cipherService;
    private final byte[][] counterBlocks;

    public ThreadTaskCipherCTR(CipherService cipherService, byte[] text, byte[] initialVector) {
        this.cipherService = cipherService;
        counterBlocks = new byte[text.length / cipherService.getBlockSizeInBytes()][cipherService.getBlockSizeInBytes()];
        byte[] counter = initialVector.clone();

        for (int i = 0; i < text.length / cipherService.getBlockSizeInBytes(); i++) {
            counterBlocks[i] = counter;
            counter = getNextCounter(counter);
        }
    }

    private byte[] getNextCounter(byte[] counter) {
        byte[][] halfParts = BytesUtil.splitInHalf(counter);
        byte[] rightPartCounter = BytesUtil.longToBytes(BytesUtil.bytesToLong(halfParts[1]) + 1, halfParts[1].length);
        return BytesUtil.mergePart(halfParts[0], rightPartCounter);
    }

    @Override
    public Pair<Integer, byte[]> apply(byte[] text, int indexBegin, int textBlockSize, int countBlocks) {
        byte[] result = new byte[countBlocks * textBlockSize];
        byte[] textBlock = new byte[textBlockSize];

        for (int i = 0; i < countBlocks; i++) {
            System.arraycopy(text, indexBegin + i * textBlockSize, textBlock, 0, textBlockSize);
            byte[] cipherBlockText = BytesUtil.xor(textBlock, cipherService.encryptBlock(counterBlocks[(indexBegin + i * textBlockSize) / textBlockSize]));
            System.arraycopy(cipherBlockText, 0, result, i * textBlockSize, textBlockSize);
        }

        return Pair.of(indexBegin, result);
    }
}
