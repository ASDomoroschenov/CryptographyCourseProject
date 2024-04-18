package ru.mai.cipher.cipher_impl.mode.cbc;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.cipher_thread.text.text_interface.TextThreadTask;
import ru.mai.cipher.utils.BitsUtil;

import java.util.concurrent.ExecutionException;

@AllArgsConstructor
public class ThreadTaskDecryptCBC implements TextThreadTask {
    private final CipherService cipherService;
    private final byte[] initializationVector;

    @Override
    public Pair<Integer, byte[]> apply(byte[] text, int indexBegin, int textBlockSize, int countBlocks) throws ExecutionException, InterruptedException {
        byte[] decryptedBlock;
        byte[] result = new byte[countBlocks * textBlockSize];
        byte[] textBlock = new byte[textBlockSize];

        if (indexBegin == 0) {
            decryptedBlock = initializationVector;
        } else {
            decryptedBlock = new byte[textBlockSize];
            System.arraycopy(text, indexBegin - textBlockSize, decryptedBlock, 0, textBlockSize);
        }

        for (int i = 0; i < countBlocks; i++) {
            System.arraycopy(text, indexBegin + i * textBlockSize, textBlock, 0, textBlockSize);
            byte[] deCipherBlockText = BitsUtil.xor(decryptedBlock, cipherService.decryptBlock(textBlock));
            System.arraycopy(deCipherBlockText, 0, result, i * textBlockSize, textBlockSize);
            decryptedBlock = textBlock.clone();
        }

        return Pair.of(indexBegin, result);
    }
}
