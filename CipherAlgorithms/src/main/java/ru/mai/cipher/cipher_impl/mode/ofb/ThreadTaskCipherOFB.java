package ru.mai.cipher.cipher_impl.mode.ofb;

import org.apache.commons.lang3.tuple.Pair;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.cipher_thread.text.text_interface.TextThreadTask;
import ru.mai.cipher.utils.BitsUtil;

public class ThreadTaskCipherOFB implements TextThreadTask {
    private final byte[][] keyBlocks;

    public ThreadTaskCipherOFB(CipherService cipherService, byte[] text, byte[] initialVector) {
        keyBlocks = new byte[text.length / cipherService.getBlockSizeInBytes()][];
        byte[] keyBlock = initialVector;

        for (int i = 0; i < text.length / cipherService.getBlockSizeInBytes(); i++) {
            keyBlocks[i] = keyBlock.clone();
            keyBlock = cipherService.encryptBlock(keyBlock);
        }
    }

    @Override
    public Pair<Integer, byte[]> apply(byte[] text, int indexBegin, int textBlockSize, int countBlocks) {
        byte[] result = new byte[countBlocks * textBlockSize];
        byte[] textBlock = new byte[textBlockSize];

        for (int i = 0; i < countBlocks; i++) {
            System.arraycopy(text, indexBegin + i * textBlockSize, textBlock, 0, textBlockSize);
            byte[] cipherBlockText = BitsUtil.xor(textBlock, keyBlocks[(indexBegin + i * textBlockSize) / textBlockSize]);
            System.arraycopy(cipherBlockText, 0, result, i * textBlockSize, textBlockSize);
        }

        return Pair.of(indexBegin, result);
    }
}
