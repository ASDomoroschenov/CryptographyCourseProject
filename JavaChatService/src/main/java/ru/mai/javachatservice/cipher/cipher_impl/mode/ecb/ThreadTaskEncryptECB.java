package ru.mai.javachatservice.cipher.cipher_impl.mode.ecb;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.javachatservice.cipher.cipher_interface.CipherService;
import ru.mai.javachatservice.cipher.cipher_thread.text.text_interface.TextThreadTask;

@AllArgsConstructor
public class ThreadTaskEncryptECB implements TextThreadTask {
    private CipherService cipherService;

    @Override
    public Pair<Integer, byte[]> apply(byte[] text, int indexBegin, int textBlockSize, int countBlocks) {
        byte[] result = new byte[countBlocks * textBlockSize];
        byte[] textBlock = new byte[textBlockSize];

        for (int i = 0; i < countBlocks; i++) {
            System.arraycopy(text, indexBegin + i * textBlockSize, textBlock, 0, textBlockSize);
            byte[] deCipherBlockText = cipherService.encryptBlock(textBlock);
            System.arraycopy(deCipherBlockText, 0, result, i * textBlockSize, textBlockSize);
        }

        return Pair.of(indexBegin, result);
    }
}