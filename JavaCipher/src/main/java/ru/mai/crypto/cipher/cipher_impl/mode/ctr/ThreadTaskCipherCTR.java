package ru.mai.crypto.cipher.cipher_impl.mode.ctr;

import org.apache.commons.lang3.tuple.Pair;
import ru.mai.crypto.cipher.cipher_interface.CipherService;
import ru.mai.crypto.cipher.cipher_thread.text.text_interface.TextThreadTask;
import ru.mai.crypto.cipher.utils.BitsUtil;

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
        Pair<byte[], byte[]> parts = BitsUtil.splitInHalf(counter);
        byte[] leftPart = parts.getLeft();
        byte[] rightPart = parts.getRight();
        byte[] rightPartCounter = BitsUtil.longToBytes(BitsUtil.bytesToLong(rightPart) + 1, rightPart.length);
        return BitsUtil.mergePart(leftPart, rightPartCounter);
    }

    @Override
    public Pair<Integer, byte[]> apply(byte[] text, int indexBegin, int textBlockSize, int countBlocks) {
        byte[] result = new byte[countBlocks * textBlockSize];
        byte[] textBlock = new byte[textBlockSize];

        for (int i = 0; i < countBlocks; i++) {
            System.arraycopy(text, indexBegin + i * textBlockSize, textBlock, 0, textBlockSize);
            byte[] cipherBlockText = BitsUtil.xor(textBlock, cipherService.encryptBlock(counterBlocks[(indexBegin + i * textBlockSize) / textBlockSize]));
            System.arraycopy(cipherBlockText, 0, result, i * textBlockSize, textBlockSize);
        }

        return Pair.of(indexBegin, result);
    }
}
