package ru.mai.cipher.cipher_impl;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.utils.BitsUtil;

import java.util.Map;

public class RC5 implements CipherService {
    private final int sizeBlockInBits;
    private final int countRounds;
    private final long[] roundKeys;
    private final RC5Utils utils;

    public RC5(int sizeKeyInBits, int sizeBlockInBits, int countRounds, byte[] key) {
        if (!Check.checkSizeBlockInBits(sizeBlockInBits)) {
            throw new IllegalArgumentException("Illegal size block in bits");
        }
        if (!Check.checkSizeKeyInBits(sizeKeyInBits)) {
            throw new IllegalArgumentException("Illegal size key in bits");
        }
        if (!Check.checkCountRounds(countRounds)) {
            throw new IllegalArgumentException("Illegal count rounds");
        }

        this.sizeBlockInBits = sizeBlockInBits;
        this.countRounds = countRounds;
        this.utils = new RC5Utils(sizeBlockInBits, sizeKeyInBits, countRounds);
        this.roundKeys = new KeyGenerator(sizeBlockInBits, key, utils).generate();
    }


    @Override
    public int getBlockSizeInBytes() {
        return sizeBlockInBits / Byte.SIZE;
    }

    @Override
    public byte[] encryptBlock(byte[] text) {
        Pair<byte[], byte[]> parts = BitsUtil.splitInHalf(text);

        long a = utils.addModulo(BitsUtil.bytesToLong(parts.getLeft()), roundKeys[0]);
        long b = utils.addModulo(BitsUtil.bytesToLong(parts.getRight()), roundKeys[1]);

        for (int i = 1; i < countRounds; i++) {
            a = utils.addModulo(BitsUtil.cyclicLeftShift((a ^ b), sizeBlockInBits / 2, b), roundKeys[2 * i]);
            b = utils.addModulo(BitsUtil.cyclicLeftShift((a ^ b), sizeBlockInBits / 2, a), roundKeys[2 * i + 1]);
        }

        return utils.collectParts(a, b, text.length);
    }

    @Override
    public byte[] decryptBlock(byte[] text) {
        Pair<byte[], byte[]> parts = BitsUtil.splitInHalf(text);
        long a = BitsUtil.bytesToLong(parts.getLeft());
        long b = BitsUtil.bytesToLong(parts.getRight());

        for (int i = countRounds - 1; i >= 1; i--) {
            b = BitsUtil.cyclicRightShift(utils.subModulo(b, roundKeys[2 * i + 1]), sizeBlockInBits / 2, a) ^ a;
            a = BitsUtil.cyclicRightShift(utils.subModulo(a, roundKeys[2 * i]), sizeBlockInBits / 2, b) ^ b;
        }

        b = utils.subModulo(b, roundKeys[1]);
        a = utils.subModulo(a, roundKeys[0]);

        return utils.collectParts(a, b, text.length);
    }

    @AllArgsConstructor
    public static class RC5Utils {
        private int sizeBlockInBits;
        private int sizeKeyInBits;
        private int countRounds;
        private static final Map<Integer, Pair<Long, Long>> CONSTANTS_RC5 = Map.of(
                16, Pair.of(0xB7E1L, 0x9E37L),
                32, Pair.of(0xB7E15163L, 0x9E3779B9L),
                64, Pair.of(0xB7E151628AED2A6BL, 0x9E3779B97F4A7C15L)
        );

        public long[] getWords(byte[] key) {
            int sizeWord = sizeBlockInBits / 2;
            int countWords = (sizeKeyInBits + sizeWord - 1) / sizeWord;
            long[] result = new long[countWords];

            for (int i = 0; i < countWords; i++) {
                result[i] = getBits(key, i * sizeWord, sizeWord);
            }

            return result;
        }

        public long[] getSArray() {
            int sizeHalfBlockInBits = sizeBlockInBits / 2;
            int countWordsSArray = 2 * (countRounds + 1);
            long p = CONSTANTS_RC5.get(sizeHalfBlockInBits).getLeft();
            long q = CONSTANTS_RC5.get(sizeHalfBlockInBits).getLeft();
            long[] result = new long[countWordsSArray];

            result[0] = p;

            for (int i = 1; i < countWordsSArray; i++) {
                result[i] = addModulo(result[i - 1], q);
            }

            return result;
        }

        public long getBits(byte[] bytes, int from, int countBits) {
            byte[] result = new byte[(countBits + Byte.SIZE - 1) / Byte.SIZE];

            for (int i = 0; i < countBits; i++) {
                if (from + i >= sizeKeyInBits) {
                    setBitFromEnd(result, i / countBits, false);
                } else {
                    setBitFromEnd(result, i, getBitFromEnd(bytes, from + i) == 1);
                }
            }

            return BitsUtil.bytesToLong(result);
        }

        public int getBitFromEnd(byte[] bytes, int indexBit) {
            return (bytes[indexBit / Byte.SIZE] >> (Byte.SIZE - indexBit % Byte.SIZE - 1)) & 1;
        }

        public void setBitFromEnd(byte[] bytes, int indexBit, boolean valueBit) {
            if (valueBit) {
                bytes[indexBit / Byte.SIZE] |= (byte) (1 << (Byte.SIZE - indexBit % Byte.SIZE - 1));
            } else {
                bytes[indexBit / Byte.SIZE] &= (byte) ~(1 << (Byte.SIZE - indexBit % Byte.SIZE - 1));
            }
        }

        public long addModulo(long first, long second) {
            long result = 0;
            long reminder = 0;

            for (int i = 0; i < sizeBlockInBits / 2; i++) {
                long tempSum = ((first >> i) & 1) ^ ((second >> i) & 1) ^ reminder;
                reminder = (((first >> i) & 1) + ((second >> i) & 1) + reminder) >> 1;
                result |= tempSum << i;
            }

            return result;
        }

        public long subModulo(long first, long second) {
            return addModulo(first, ~second + 1);
        }

        public byte[] collectParts(long left, long right, int sizeResult) {
            byte[] leftResult = new byte[sizeResult / 2];
            byte[] rightResult = new byte[sizeResult / 2];

            for (int i = 0; i < sizeResult / 2; i++) {
                leftResult[sizeResult / 2 - i - 1] = (byte) ((left >> (i * Byte.SIZE)) & ((1 << Byte.SIZE) - 1));
                rightResult[sizeResult / 2 - i - 1] = (byte) ((right >> (i * Byte.SIZE)) & ((1 << Byte.SIZE) - 1));
            }

            byte[] result = new byte[sizeResult];

            System.arraycopy(leftResult, 0, result, 0, sizeResult / 2);
            System.arraycopy(rightResult, 0, result, sizeResult / 2, sizeResult / 2);

            return result;
        }
    }

    @AllArgsConstructor
    static class KeyGenerator {
        private final int sizeBlockInBits;
        private final byte[] key;
        private final RC5Utils utils;

        public long[] generate() {
            long[] lArray = utils.getWords(key);
            long[] sArray = utils.getSArray();
            int sizeHalfBlockInBits = sizeBlockInBits / 2;
            int countWordsSArray = sArray.length;
            int countWords = lArray.length;

            int i = 0;
            int j = 0;
            long a = 0;
            long b = 0;

            for (int counter = 0; counter < 3 * Integer.max(countWordsSArray, countWords); counter++) {
                a = sArray[i] = BitsUtil.cyclicLeftShift(utils.addModulo(utils.addModulo(sArray[i], a), b), sizeHalfBlockInBits, 3);
                b = lArray[j] = BitsUtil.cyclicLeftShift(utils.addModulo(utils.addModulo(sArray[i], a), b), sizeHalfBlockInBits, utils.addModulo(a, b));
                i = (i + 1) % countWordsSArray;
                j = (j + 1) % countWords;
            }

            return sArray;
        }
    }

    static class Check {
        private Check() {
        }

        public static boolean checkSizeBlockInBits(int sizeBlockInBits) {
            return sizeBlockInBits == 32 || sizeBlockInBits == 64 || sizeBlockInBits == 128;
        }

        public static boolean checkSizeKeyInBits(int sizeKeyInBits) {
            return sizeKeyInBits > 0 && sizeKeyInBits < 256;
        }

        public static boolean checkCountRounds(int countRounds) {
            return countRounds > 0 && countRounds < 256;
        }
    }
}