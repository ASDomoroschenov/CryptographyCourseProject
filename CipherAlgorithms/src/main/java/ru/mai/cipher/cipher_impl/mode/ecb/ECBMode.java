package ru.mai.cipher.cipher_impl.mode.ecb;

import lombok.AllArgsConstructor;
import ru.mai.cipher.cipher_interface.CipherMode;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.cipher_thread.text.text_impl.CollectTextImpl;
import ru.mai.cipher.cipher_thread.text.text_impl.TextThreadCipherImpl;

import java.util.concurrent.ExecutionException;

@AllArgsConstructor
public class ECBMode implements CipherMode {
    private final CipherService cipherService;

    @Override
    public byte[] encrypt(byte[] text) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (text == null || text.length == 0) {
            throw new IllegalArgumentException("Illegal bytes text");
        }

        return new TextThreadCipherImpl(
                cipherService.getBlockSizeInBytes(),
                new ThreadTaskEncryptECB(cipherService),
                new CollectTextImpl()
        ).cipher(text);
    }

    @Override
    public byte[] decrypt(byte[] text) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (text == null || text.length == 0) {
            throw new IllegalArgumentException("Illegal bytes text");
        }

        return new TextThreadCipherImpl(
                cipherService.getBlockSizeInBytes(),
                new ThreadTaskDecryptECB(cipherService),
                new CollectTextImpl()
        ).cipher(text);
    }
}
