package ru.mai.cipher.cipher_impl.mode.cfb;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.cipher_thread.text.text_interface.TextThreadTask;
import ru.mai.cipher.utils.BytesUtil;

@AllArgsConstructor
public class ThreadTaskDecryptCFB implements TextThreadTask {
    private final CipherService cipher;
    private final byte[] initialVector;

    @Override
    public Pair<Integer, byte[]> apply(byte[] text, int indexBegin, int textBlockSize, int countBlocks) {
        byte[] decryptedBlock;
        byte[] result = new byte[countBlocks * textBlockSize];
        byte[] textBlock = new byte[textBlockSize];

        if (indexBegin == 0) {
            decryptedBlock = initialVector;
        } else {
            decryptedBlock = new byte[textBlockSize];
            System.arraycopy(text, indexBegin - textBlockSize, decryptedBlock, 0, textBlockSize);
        }

        for (int i = 0; i < countBlocks; i++) {
            System.arraycopy(text, indexBegin + i * textBlockSize, textBlock, 0, textBlockSize);
            byte[] deCipherBlockText = BytesUtil.xor(cipher.encryptBlock(decryptedBlock), textBlock);
            System.arraycopy(deCipherBlockText, 0, result, i * textBlockSize, textBlockSize);
            decryptedBlock = textBlock.clone();
        }

        return Pair.of(indexBegin, result);
    }
}