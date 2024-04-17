package ru.mai.cipher;

import lombok.extern.slf4j.Slf4j;
import ru.mai.cipher.cipher_impl.mode.cbc.CBCMode;
import ru.mai.cipher.cipher_impl.mode.cfb.CFBMode;
import ru.mai.cipher.cipher_impl.mode.ctr.CTRMode;
import ru.mai.cipher.cipher_impl.mode.ecb.ECBMode;
import ru.mai.cipher.cipher_impl.mode.ofb.OFBMode;
import ru.mai.cipher.cipher_impl.mode.pcbc.PCBCMode;
import ru.mai.cipher.cipher_impl.mode.rd.RDMode;
import ru.mai.cipher.cipher_impl.padding.Padding;
import ru.mai.cipher.cipher_impl.padding.impl.ANSIX923Padding;
import ru.mai.cipher.cipher_impl.padding.impl.ISO10126Padding;
import ru.mai.cipher.cipher_impl.padding.impl.PKCS7Padding;
import ru.mai.cipher.cipher_interface.CipherMode;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.cipher_thread.file.file_impl.FileThreadCipherImpl;
import ru.mai.cipher.cipher_thread.file.file_impl.FileThreadTaskCipherImpl;
import ru.mai.cipher.cipher_thread.file.file_interface.FileThreadCipher;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@Slf4j
public class Cipher {
    public enum PaddingMode {
        ANSIX923,
        ISO10126,
        PKCS7
    }

    public enum EncryptionMode {
        ECB,
        CBC,
        PCBC,
        CFB,
        OFB,
        CTR,
        RD
    }

    private final byte[] initializationVector;
    private final CipherService cipherService;
    private final Padding padding;
    private final CipherMode cipherMode;

    public Cipher(byte[] initializationVector, CipherService cipherService, PaddingMode paddingMode, EncryptionMode encryptionMode) {
        if (initializationVector.length != cipherService.getBlockSizeInBytes()) {
            throw new IllegalArgumentException("Initialization vector must be equals block size");
        }

        this.initializationVector = initializationVector;
        this.cipherService = cipherService;
        this.padding = getPadding(paddingMode);
        this.cipherMode = getCipherMode(encryptionMode);
    }

    private Padding getPadding(PaddingMode paddingMode) {
        return switch (paddingMode) {
            case ISO10126 -> new ISO10126Padding();
            case ANSIX923 -> new ANSIX923Padding();
            case PKCS7 -> new PKCS7Padding();
        };
    }

    private CipherMode getCipherMode(EncryptionMode encryptionMode) {
        return switch (encryptionMode) {
            case ECB -> new ECBMode(cipherService);
            case CBC -> new CBCMode(cipherService, initializationVector);
            case PCBC -> new PCBCMode(cipherService, initializationVector);
            case CFB -> new CFBMode(cipherService, initializationVector);
            case OFB -> new OFBMode(cipherService, initializationVector);
            case CTR -> new CTRMode(cipherService, initializationVector);
            case RD -> new RDMode(cipherService, initializationVector);
        };
    }

    public byte[] encrypt(byte[] text) throws ExecutionException, InterruptedException {
        try {
            return cipherMode.encrypt(padding.addPadding(text, cipherService.getBlockSizeInBytes()));
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }

        return new byte[0];
    }

    public String encryptFile(String pathToInputFile) {
        String encryptFile = null;
        int sizeBlockBytes = cipherService.getBlockSizeInBytes();

        try {
            String fileWithPadding = padding.addPadding(pathToInputFile, sizeBlockBytes);
            encryptFile = new FileThreadCipherImpl(
                    sizeBlockBytes,
                    new FileThreadTaskCipherImpl(cipherMode)
            ).cipher(fileWithPadding, addPostfixToFileName(pathToInputFile, "_enc"), FileThreadCipher.CipherAction.ENCRYPT);

            Files.delete(Path.of(fileWithPadding));
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
            log.error(Arrays.toString(ex.getStackTrace()));
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error(Arrays.toString(ex.getStackTrace()));
        }

        return encryptFile;
    }

    public byte[] decrypt(byte[] text) throws ExecutionException, InterruptedException {
        try {
            return padding.removePadding(cipherMode.decrypt(text));
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }

        return new byte[0];
    }

    public String decryptFile(String pathToInputFile) {
        String decryptFile = null;
        int sizeBlockBytes = cipherService.getBlockSizeInBytes();

        try {
            decryptFile = new FileThreadCipherImpl(
                    sizeBlockBytes,
                    new FileThreadTaskCipherImpl(cipherMode)
            ).cipher(pathToInputFile, addPostfixToFileName(pathToInputFile, "_dec"), FileThreadCipher.CipherAction.DECRYPT);
            String removePaddingFile = padding.removePadding(decryptFile);

            if (!(new File(removePaddingFile).renameTo(new File(decryptFile)))) {
                log.error("Error while renaming file");
            }
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
            log.error(Arrays.toString(ex.getStackTrace()));
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error(Arrays.toString(ex.getStackTrace()));
        }

        return decryptFile;
    }

    private String addPostfixToFileName(String pathToInputFile, String postfix) {
        int dotIndex = pathToInputFile.lastIndexOf('.');
        String baseName = pathToInputFile.substring(0, dotIndex);
        String extension = pathToInputFile.substring(dotIndex);
        return baseName + postfix + extension;
    }
}
