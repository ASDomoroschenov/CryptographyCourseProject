package ru.mai.cipher.utils;

import org.apache.commons.lang3.tuple.Pair;

public class BitsUtil {
    private BitsUtil() {}

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

    public static long bytesToLong(byte[] bytesValue) {
        long result = 0L;

        for (byte byteValue : bytesValue) {
            long longValue = convertByteToLong(byteValue);
            result = (result << Byte.SIZE) | longValue;
        }

        return result;
    }

    public static long convertByteToLong(byte byteValue) {
        int signBit = (byteValue >> (Byte.SIZE - 1)) & 1;
        long longValue = byteValue & ((1 << (Byte.SIZE - 1)) - 1);

        if (signBit == 1) {
            longValue |= 1 << (Byte.SIZE - 1);
        }

        return longValue;
    }

    public static long cyclicLeftShift(long number, int numBits, long k) {
        long valueShift = Math.abs(k % numBits);
        return (number << valueShift) | ((number & (((1L << valueShift) - 1) << (numBits - valueShift))) >>> (numBits - valueShift));
    }

    public static long cyclicRightShift(long number, int numBits, long k) {
        long valueShift = Math.abs(k % numBits);
        return (number >>> valueShift) | ((number & ((1L << valueShift) - 1)) << (numBits - valueShift));
    }

    public static byte[] mergePart(byte[] left, byte[] right) {
        if (left != null && right != null) {
            byte[] result = new byte[left.length + right.length];

            System.arraycopy(left, 0, result, 0, left.length);
            System.arraycopy(right, 0, result, left.length, right.length);

            return result;
        }

        return new byte[0];
    }

    public static Pair<byte[], byte[]> splitInHalf(byte[] bytes) {
        if (bytes != null) {
            byte[][] splitHalfParts = new byte[2][bytes.length / 2];

            System.arraycopy(bytes, 0, splitHalfParts[0], 0, bytes.length / 2);
            System.arraycopy(bytes, bytes.length / 2, splitHalfParts[1], 0, bytes.length / 2);

            return Pair.of(splitHalfParts[0], splitHalfParts[1]);
        }

        return null;
    }
}
