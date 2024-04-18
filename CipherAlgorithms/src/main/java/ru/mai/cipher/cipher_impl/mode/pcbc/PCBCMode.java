package ru.mai.cipher.cipher_impl.mode.pcbc;

import lombok.AllArgsConstructor;
import ru.mai.cipher.cipher_interface.CipherMode;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.utils.BitsUtil;

@AllArgsConstructor
public class PCBCMode implements CipherMode {
    private final CipherService cipherService;
    private final byte[] initialVector;

    @Override
    public byte[] encrypt(byte[] text) throws IllegalArgumentException {
        if (text == null || text.length == 0) {
            throw new IllegalArgumentException("Illegal bytes text");
        }

        int textBlockSize = cipherService.getBlockSizeInBytes();
        byte[] textBlock = new byte[textBlockSize];
        byte[] prevTextBlock = new byte[textBlockSize];
        byte[] cipherBlock = initialVector;
        byte[] result = new byte[text.length];

        for (int i = 0; i < text.length; i += textBlockSize) {
            System.arraycopy(text, i, textBlock, 0, textBlockSize);
            cipherBlock = cipherService.encryptBlock(BitsUtil.xor(BitsUtil.xor(textBlock, prevTextBlock), cipherBlock));
            prevTextBlock = textBlock.clone();
            System.arraycopy(cipherBlock, 0, result, i, textBlockSize);
        }

        return result;
    }

    @Override
    public byte[] decrypt(byte[] text) throws IllegalArgumentException {
        if (text == null || text.length == 0) {
            throw new IllegalArgumentException("Illegal bytes text");
        }

        int textBlockSize = cipherService.getBlockSizeInBytes();
        byte[] textBlock = new byte[textBlockSize];
        byte[] prevCipherBlock = new byte[textBlockSize];
        byte[] deCipherBlock = initialVector;
        byte[] result = new byte[text.length];

        for (int i = 0; i < text.length; i += textBlockSize) {
            System.arraycopy(text, i, textBlock, 0, textBlockSize);
            deCipherBlock = BitsUtil.xor(BitsUtil.xor(cipherService.decryptBlock(textBlock), prevCipherBlock), deCipherBlock);
            prevCipherBlock = textBlock.clone();
            System.arraycopy(deCipherBlock, 0, result, i, textBlockSize);
        }

        return result;
    }
}
