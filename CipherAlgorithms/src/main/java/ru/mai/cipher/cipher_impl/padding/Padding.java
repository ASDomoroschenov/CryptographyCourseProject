package ru.mai.cipher.cipher_impl.padding;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class Padding {
    public byte[] addPadding(byte[] text, int blockSizeInBytes) {
        if (text == null) {
            throw new IllegalArgumentException("Illegal bytes text");
        }
        if (blockSizeInBytes <= 0) {
            throw new IllegalArgumentException("Illegal text block size");
        }

        byte valuePadding = (byte) (blockSizeInBytes - text.length % blockSizeInBytes);
        byte[] textWithPadding = new byte[text.length + valuePadding];
        byte[] paddingArray = getArrayPadding(valuePadding);

        System.arraycopy(text, 0, textWithPadding, 0, text.length);
        System.arraycopy(paddingArray, 0, textWithPadding, text.length, paddingArray.length);

        return textWithPadding;
    }

    public byte[] removePadding(byte[] text) {
        if (text == null || text.length == 0) {
            throw new IllegalArgumentException("Illegal bytes text");
        }

        byte valuePadding = text[text.length - 1];

        if (valuePadding > text.length) {
            throw new IllegalArgumentException("Illegal size padding");
        }

        byte[] textWithoutPadding = new byte[text.length - valuePadding];

        System.arraycopy(text, 0, textWithoutPadding, 0, text.length - valuePadding);

        return textWithoutPadding;
    }

    public String addPadding(String pathToFile, int blockSizeInBytes) throws IOException {
        String pathToAddPaddingFile = addPostfixToFileName(pathToFile, "_add_padding");

        try {
            FileUtils.copyFile(new File(pathToFile), new File(pathToAddPaddingFile));
        } catch (IOException ex) {
            throw new IOException("Error while copying files");
        }

        try (RandomAccessFile inputFile = new RandomAccessFile(pathToFile, "r");
             RandomAccessFile paddingFile = new RandomAccessFile(pathToAddPaddingFile, "rw")) {
            paddingFile.seek(inputFile.length());
            byte[] padding = getArrayPadding((byte) (blockSizeInBytes - inputFile.length() % blockSizeInBytes));
            paddingFile.write(padding);
        }

        return pathToAddPaddingFile;
    }

    public String removePadding(String pathToFile) throws IOException {
        String pathToRemovePaddingFile = addPostfixToFileName(pathToFile, "_remove_padding");
        byte valuePadding;
        byte[] buffer;

        try (RandomAccessFile inputFile = new RandomAccessFile(pathToFile, "r")) {
            inputFile.seek(inputFile.length() - 1);
            valuePadding = inputFile.readByte();
            buffer = new byte[(int) (inputFile.length() - valuePadding)];
        }

        try (RandomAccessFile inputFile = new RandomAccessFile(pathToFile, "r");
             RandomAccessFile paddingFile = new RandomAccessFile(pathToRemovePaddingFile, "rw")) {
            inputFile.read(buffer);
            paddingFile.write(buffer);
        }

        return pathToRemovePaddingFile;
    }

    private String addPostfixToFileName(String fileName, String postfix) {
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = fileName.substring(0, dotIndex);
        String extension = fileName.substring(dotIndex);
        return baseName + postfix + extension;
    }

    protected abstract byte[] getArrayPadding(byte valuePadding);
}
