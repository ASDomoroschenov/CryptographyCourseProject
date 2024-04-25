package ru.mai.crypto.cipher.cipher_interface;

public interface CipherService {
    int getBlockSizeInBytes();

    byte[] encryptBlock(byte[] text);

    byte[] decryptBlock(byte[] text);
}
