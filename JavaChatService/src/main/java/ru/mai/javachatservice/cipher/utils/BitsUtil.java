package ru.mai.javachatservice.cipher.utils;

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

    public static int convertCharToInt(char charValue) {
        int signBit = (charValue >> (Character.SIZE - 1)) & 1;
        int intValue = charValue & ((1 << (Character.SIZE - 1)) - 1);

        if (signBit == 1) {
            intValue |= 1 << (Character.SIZE - 1);
        }

        return intValue;
    }

    public static byte[] permutation(byte[] arrayBits, int[] permutationValues) throws IllegalArgumentException {
        byte[] permutationResult = new byte[(permutationValues.length + Byte.SIZE - 1) / Byte.SIZE];

        for (int i = 0; i < permutationValues.length; i++) {
            int indexBlock = (Long.SIZE - permutationValues[i] - 1) / Byte.SIZE;
            int indexBitInBlock = Byte.SIZE - permutationValues[i] % Byte.SIZE - 1;
            byte bit = (byte) ((arrayBits[indexBlock] >> (Byte.SIZE - indexBitInBlock - 1)) & 1);
            permutationResult[i / Byte.SIZE] |= (byte) (bit << (Byte.SIZE - (i % Byte.SIZE) - 1));
        }

        return permutationResult;
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

    public static long addModulo(long first, long second, int numBits) {
        long result = 0;
        long reminder = 0;

        for (int i = 0; i < numBits; i++) {
            long tempSum = ((first >> i) & 1) ^ ((second >> i) & 1) ^ reminder;
            reminder = (((first >> i) & 1) + ((second >> i) & 1) + reminder) >> 1;
            result |= tempSum << i;
        }

        return result;
    }

    public static long subModulo(long first, long second, int numBits) {
        return addModulo(first, ~second + 1, numBits);
    }

    public static long getBits(byte[] bytes, int from, int countBits) {
        byte[] result = new byte[(countBits + Byte.SIZE - 1) / Byte.SIZE];

        for (int i = 0; i < countBits; i++) {
            if (from + i >= bytes.length * Byte.SIZE) {
                setBitFromEnd(result, i / countBits, false);
            } else {
                setBitFromEnd(result, i, getBitFromEnd(bytes, from + i) == 1);
            }
        }

        return BitsUtil.bytesToLong(result);
    }

    public static long getBits(long block, int from, int countBits) {
        return (block << (Long.SIZE - from - 1) >>> (Long.SIZE - countBits));
    }

    public static int getBitFromEnd(byte[] bytes, int indexBit) {
        return (bytes[indexBit / Byte.SIZE] >> (Byte.SIZE - indexBit % Byte.SIZE - 1)) & 1;
    }

    public static void setBitFromEnd(byte[] bytes, int indexBit, boolean valueBit) {
        if (valueBit) {
            bytes[indexBit / Byte.SIZE] |= (byte) (1 << (Byte.SIZE - indexBit % Byte.SIZE - 1));
        } else {
            bytes[indexBit / Byte.SIZE] &= (byte) ~(1 << (Byte.SIZE - indexBit % Byte.SIZE - 1));
        }
    }
}
