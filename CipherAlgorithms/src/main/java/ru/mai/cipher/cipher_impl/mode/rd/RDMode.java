package ru.mai.cipher.cipher_impl.mode.rd;

import lombok.AllArgsConstructor;
import ru.mai.cipher.cipher_interface.CipherMode;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.cipher_thread.text.text_impl.CollectTextImpl;
import ru.mai.cipher.cipher_thread.text.text_impl.TextThreadCipherImpl;

import java.util.concurrent.ExecutionException;

@AllArgsConstructor
public class RDMode implements CipherMode {
    private final CipherService cipherService;
    private final byte[] initialVector;

    @Override
    public byte[] encrypt(byte[] text) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (text == null || text.length == 0) {
            throw new IllegalArgumentException("Illegal bytes text");
        }

        return new TextThreadCipherImpl(
                cipherService.getBlockSizeInBytes(),
                new ThreadTaskEncryptRD(cipherService, text, initialVector),
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
                new ThreadTaskDecryptRD(cipherService, text, initialVector),
                new CollectTextImpl()
        ).cipher(text);
    }
}
