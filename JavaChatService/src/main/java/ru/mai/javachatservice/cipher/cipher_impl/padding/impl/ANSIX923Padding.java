package ru.mai.javachatservice.cipher.cipher_impl.padding.impl;

import ru.mai.javachatservice.cipher.cipher_impl.padding.Padding;

public class ANSIX923Padding extends Padding {
    @Override
    protected byte[] getArrayPadding(byte valuePadding) {
        byte[] padding = new byte[valuePadding];

        for (int i = 0; i < padding.length - 1; i++) {
            padding[i] = 0;
        }

        padding[padding.length - 1] = valuePadding;

        return padding;
    }
}
