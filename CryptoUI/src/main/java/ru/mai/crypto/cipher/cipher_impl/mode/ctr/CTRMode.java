package ru.mai.crypto.cipher.cipher_impl.mode.ctr;

import lombok.AllArgsConstructor;
import ru.mai.crypto.cipher.cipher_interface.CipherMode;
import ru.mai.crypto.cipher.cipher_interface.CipherService;
import ru.mai.crypto.cipher.cipher_thread.text.text_impl.CollectTextImpl;
import ru.mai.crypto.cipher.cipher_thread.text.text_impl.TextThreadCipherImpl;

import java.util.concurrent.ExecutionException;

@AllArgsConstructor
public class CTRMode implements CipherMode {
    private final CipherService cipherService;
    private final byte[] initialVector;

    @Override
    public byte[] encrypt(byte[] text) throws IllegalArgumentException, ExecutionException, InterruptedException {
        return cipher(text);
    }

    @Override
    public byte[] decrypt(byte[] text) throws IllegalArgumentException, ExecutionException, InterruptedException {
        return cipher(text);
    }

    public byte[] cipher(byte[] text) throws ExecutionException, InterruptedException {
        if (text == null || text.length == 0) {
            throw new IllegalArgumentException("Illegal bytes text");
        }

        return new TextThreadCipherImpl(
                cipherService.getBlockSizeInBytes(),
                new ThreadTaskCipherCTR(cipherService, text, initialVector),
                new CollectTextImpl()
        ).cipher(text);
    }
}
