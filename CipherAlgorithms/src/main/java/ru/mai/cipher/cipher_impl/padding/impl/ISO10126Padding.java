package ru.mai.cipher.cipher_impl.padding.impl;

import ru.mai.cipher.cipher_impl.padding.Padding;

import java.util.Random;

public class ISO10126Padding extends Padding {
    private final Random random = new Random();

    @Override
    protected byte[] getArrayPadding(byte valuePadding) {
        byte[] padding = new byte[valuePadding];

        for (int i = 0; i < padding.length - 1; i++) {
            padding[i] = (byte) (random.nextInt(Byte.MAX_VALUE - Byte.MIN_VALUE) + Byte.MIN_VALUE);
        }

        padding[padding.length - 1] = valuePadding;

        return padding;
    }
}
