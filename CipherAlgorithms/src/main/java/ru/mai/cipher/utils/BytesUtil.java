package ru.mai.cipher.utils;

public class BytesUtil {
    public static byte[] xor(byte[] first, byte[] second) {
        int maxLength = Integer.max(first.length, second.length);
        byte[] result = new byte[maxLength];

        for (int i = 0; i < maxLength; i++) {
            byte firstByte = first.length - i - 1 >= 0 ? first[first.length - i - 1] : 0;
            byte secondByte = second.length - i - 1 >= 0 ? second[second.length - i - 1] : 0;
            result[maxLength - i - 1] = (byte) (firstByte ^ secondByte);
        }

        return result;
    }

    public static byte[] longToBytes(long number, int countBytes) {
        byte[] result = new byte[countBytes];

        for (int i = countBytes - 1; i >= 0; i--) {
            result[i] = (byte) (number & ((1 << Long.BYTES) - 1));
            number >>= Byte.SIZE;
        }

        return result;
    }

    public static long bytesToLong(byte[] bytes) {
        if (bytes.length > 8) {
            throw new IllegalArgumentException("Can't convert a byte array whose length exceeds 8");
        }

        long result = 0;

        for (byte byteItem : bytes) {
            result <<= Byte.SIZE;
            result |= byteItem;
        }

        return result;
    }

    public static byte[] permutation(byte[] arrayBits, int[] permutationValues) throws IllegalArgumentException {
        byte[] permutationResult = new byte[(permutationValues.length + Byte.SIZE - 1) / Byte.SIZE];

        for (int i = 0; i < permutationValues.length; i++) {
            int indexBlock = (permutationValues[i] - 1) / Byte.SIZE;
            int indexBitInBlock = permutationValues[i] - indexBlock * Byte.SIZE;
            byte bit = (byte) ((arrayBits[indexBlock] >> (Byte.SIZE - indexBitInBlock)) & 1);
            permutationResult[i / Byte.SIZE] |= (byte) (bit << (Byte.SIZE - (i % Byte.SIZE) - 1));
        }

        return permutationResult;
    }

    public static byte[] mergePart(byte[] left, byte[] right) {
        if (left != null && right != null) {
            byte[] result = new byte[left.length + right.length];

            System.arraycopy(left, 0, result, 0, left.length);
            System.arraycopy(right, 0, result, left.length, right.length);

            return result;
        }

        return null;
    }

    public static byte[][] splitInHalf(byte[] bytes) {
        if (bytes != null) {
            byte[][] splitHalfParts = new byte[2][bytes.length / 2];

            System.arraycopy(bytes, 0, splitHalfParts[0], 0, bytes.length / 2);
            System.arraycopy(bytes, bytes.length / 2, splitHalfParts[1], 0, bytes.length / 2);

            return splitHalfParts;
        }

        return null;
    }
}
