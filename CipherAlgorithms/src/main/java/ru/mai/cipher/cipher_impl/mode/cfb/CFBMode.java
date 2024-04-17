package ru.mai.cipher.cipher_impl.mode.cfb;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mai.cipher.cipher_interface.CipherMode;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.cipher_thread.text.text_impl.CollectTextImpl;
import ru.mai.cipher.cipher_thread.text.text_impl.TextThreadCipherImpl;
import ru.mai.cipher.utils.BytesUtil;

import java.util.concurrent.ExecutionException;

@Slf4j
@AllArgsConstructor
public class CFBMode implements CipherMode {
    private final CipherService cipherService;
    private final byte[] initialVector;

    @Override
    public byte[] encrypt(byte[] text) throws IllegalArgumentException {
        if (text == null || text.length == 0) {
            throw new IllegalArgumentException("Illegal bytes text");
        }

        int textBlockSize = cipherService.getBlockSizeInBytes();
        byte[] cipherBlock = initialVector;
        byte[] result = new byte[text.length];
        byte[] textBlock = new byte[textBlockSize];

        for (int i = 0; i < text.length; i += textBlockSize) {
            System.arraycopy(text, i, textBlock, 0, textBlockSize);
            byte[] cipherBlockText = BytesUtil.xor(cipherService.encryptBlock(cipherBlock), textBlock);
            System.arraycopy(cipherBlockText, 0, result, i, textBlockSize);
            cipherBlock = cipherBlockText;
        }

        return result;
    }

    @Override
    public byte[] decrypt(byte[] text) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (text == null || text.length == 0) {
            throw new IllegalArgumentException("Illegal bytes text");
        }

        return new TextThreadCipherImpl(
                cipherService.getBlockSizeInBytes(),
                new ThreadTaskDecryptCFB(cipherService, initialVector),
                new CollectTextImpl()
        ).cipher(text);
    }
}