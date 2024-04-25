package ru.mai.crypto.cipher.cipher_impl.padding.impl;

import ru.mai.crypto.cipher.cipher_impl.padding.Padding;

import java.util.Arrays;

public class PKCS7Padding extends Padding {
    @Override
    protected byte[] getArrayPadding(byte valuePadding) {
        byte[] padding = new byte[valuePadding];
        Arrays.fill(padding, valuePadding);
        return padding;
    }
}
