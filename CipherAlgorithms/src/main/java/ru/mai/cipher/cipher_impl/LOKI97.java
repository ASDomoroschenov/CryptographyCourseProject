package ru.mai.cipher.cipher_impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.cipher.cipher_interface.CipherService;
import ru.mai.cipher.utils.BitsUtil;
import ru.mai.cipher.utils.GF;

import java.util.List;

@Slf4j
public class LOKI97 implements CipherService {
    private final long[] roundKeys;
    private static final int SIZE_BLOCK_IN_BITS = 128;
    private static final int SIZE_BLOCK_IN_BYTES = 16;
    private static final long DELTA = 0x9E3779B97F4A7C15L;
    private static final int[] PERMUTATION = {
            56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1,
            58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 27, 19, 11, 3,
            60, 52, 44, 36, 28, 20, 12, 4, 61, 53, 45, 37, 29, 21, 13, 5,
            62, 54, 46, 38, 30, 22, 14, 6, 63, 55, 47, 39, 31, 23, 15, 7
    };
    private static final List<Pair<Integer, Integer>> expansion = List.of(
            Pair.of(4, 0),
            Pair.of(63, 56),
            Pair.of(58, 48),
            Pair.of(52, 40),
            Pair.of(42, 32),
            Pair.of(34, 24),
            Pair.of(28, 16),
            Pair.of(18, 8),
            Pair.of(12, 0)
    );
    private static final List<Pair<Integer, Integer>> B_BITS = List.of(
            Pair.of(63, 61),
            Pair.of(60, 58),
            Pair.of(57, 53),
            Pair.of(52, 48),
            Pair.of(47, 45),
            Pair.of(44, 42),
            Pair.of(41, 37),
            Pair.of(36, 32)
    );
    private static final List<Pair<Integer, Integer>> P_BITS = List.of(
            Pair.of(63, 56),
            Pair.of(55, 48),
            Pair.of(47, 40),
            Pair.of(39, 32),
            Pair.of(31, 24),
            Pair.of(23, 16),
            Pair.of(15, 8),
            Pair.of(7, 0)
    );

    public LOKI97(byte[] key, int sizeKeyInBits) {
        if (!Check.checkKey(key, sizeKeyInBits)) {
            throw new IllegalArgumentException("Invalid key");
        }

        this.roundKeys = new KeyGenerator(key, sizeKeyInBits, SIZE_BLOCK_IN_BYTES).getKeys();
    }

    @Override
    public int getBlockSizeInBytes() {
        return SIZE_BLOCK_IN_BITS / Byte.SIZE;
    }

    @Override
    public byte[] encryptBlock(byte[] text) {
        Pair<byte[], byte[]> parts = BitsUtil.splitInHalf(text);
        long leftPart = BitsUtil.bytesToLong(parts.getLeft());
        long rightPart = BitsUtil.bytesToLong(parts.getRight());

        for (int i = 1; i <= 16; i++) {
            long prevRight = rightPart;
            long prevLeft = leftPart;
            rightPart = prevLeft ^ LOKI97Utils.f(BitsUtil.addModulo(prevRight, roundKeys[3 * i - 2], Long.SIZE), roundKeys[3 * i - 1]);
            leftPart = BitsUtil.addModulo(BitsUtil.addModulo(prevRight, roundKeys[3 * i - 2], Long.SIZE), roundKeys[3 * i], Long.SIZE);
        }

        return BitsUtil.mergePart(BitsUtil.longToBytes(rightPart, Byte.SIZE), BitsUtil.longToBytes(leftPart, Byte.SIZE));
    }

    @Override
    public byte[] decryptBlock(byte[] text) {
        Pair<byte[], byte[]> parts = BitsUtil.splitInHalf(text);
        long rightPart = BitsUtil.bytesToLong(parts.getLeft());
        long leftPart = BitsUtil.bytesToLong(parts.getRight());

        for (int i = 16; i >= 1; i--) {
            long nextRight = rightPart;
            long nextLeft = leftPart;
            leftPart = nextRight ^ LOKI97Utils.f(BitsUtil.subModulo(nextLeft, roundKeys[3 * i], Long.SIZE), roundKeys[3 * i - 1]);
            rightPart = BitsUtil.subModulo(BitsUtil.subModulo(nextLeft, roundKeys[3 * i], Long.SIZE), roundKeys[3 * i - 2], Long.SIZE);
        }

        return BitsUtil.mergePart(BitsUtil.longToBytes(leftPart, Byte.SIZE), BitsUtil.longToBytes(rightPart, Byte.SIZE));
    }

    @AllArgsConstructor
    static class KeyGenerator {
        private final byte[] key;
        private final int sizeKeyInBits;
        private final int sizeBlockInBytes;

        public long[] getKeys() {
            long[] result = new long[49];
            long[] initialKeys = keyInitialization();

            long k4 = initialKeys[0];
            long k3 = initialKeys[1];
            long k2 = initialKeys[2];
            long k1 = initialKeys[3];

            for (int i = 1; i <= 48; i++) {
                long tempK1 = k1;
                k1 = k4 ^ LOKI97Utils.g(i, k1, k3, k2);
                k4 = k3;
                k3 = k2;
                k2 = tempK1;
                result[i] = k1;
            }

            return result;
        }

        public long[] keyInitialization() {
            byte[] wordKey1 = new byte[sizeBlockInBytes / 2];
            byte[] wordKey2 = new byte[sizeBlockInBytes / 2];
            byte[] wordKey3 = new byte[sizeBlockInBytes / 2];
            byte[] wordKey4 = new byte[sizeBlockInBytes / 2];

            System.arraycopy(key, 0, wordKey1, 0, sizeBlockInBytes / 2);
            System.arraycopy(key, sizeBlockInBytes / 2, wordKey2, 0, sizeBlockInBytes / 2);

            long key1 = BitsUtil.bytesToLong(wordKey1);
            long key2 = BitsUtil.bytesToLong(wordKey2);
            long key3 = 0;
            long key4 = 0;

            if (sizeKeyInBits == 256) {
                System.arraycopy(key, 2 * (sizeBlockInBytes / 2), wordKey3, 0, sizeBlockInBytes / 2);
                System.arraycopy(key, 3 * (sizeBlockInBytes / 2), wordKey4, 0, sizeBlockInBytes / 2);
                key3 = BitsUtil.bytesToLong(wordKey3);
                key4 = BitsUtil.bytesToLong(wordKey4);
            } else if (sizeKeyInBits == 192) {
                System.arraycopy(key, 2 * (sizeBlockInBytes / 2), wordKey3, 0, sizeBlockInBytes / 2);
                key4 = LOKI97Utils.f(key1, key2);
            } else if (sizeKeyInBits == 128) {
                key3 = LOKI97Utils.f(key2, key1);
                key4 = LOKI97Utils.f(key1, key2);
            }

            return new long[]{key1, key2, key3, key4};
        }
    }

    static class LOKI97Utils {
        private LOKI97Utils() {}

        public static long g(int index, long key1, long key3, long key2) {
            return f(key1 + key3 + DELTA * index, key2);
        }

        public static long f(long first, long second) {
            long bRight = BitsUtil.getBits(second, 31, 32);
            long kp = KP(first, bRight);
            char[] e = E(kp);
            byte[] sa = SA(e);
            long p = P(BitsUtil.bytesToLong(sa));

            char[] blockForSb = new char[Byte.SIZE];

            for (int i = 0; i < Byte.SIZE; i++) { // +
                char bitsB = (char) BitsUtil.getBits(second, B_BITS.get(i).getLeft(), B_BITS.get(i).getLeft() - B_BITS.get(i).getRight() + 1);
                char bitsA = (char) BitsUtil.getBits(p, P_BITS.get(i).getLeft(), P_BITS.get(i).getLeft() - P_BITS.get(i).getRight() + 1);
                blockForSb[i] = (char) ((bitsA << (B_BITS.get(i).getLeft() - B_BITS.get(i).getRight() + 1)) | bitsB);
            }

            return BitsUtil.bytesToLong(SB(blockForSb));
        }

        public static char[] E(long block) {
            char[] result = new char[Byte.SIZE];

            result[0] = (char) BitsUtil.getBits(block, expansion.get(0).getLeft(), expansion.get(0).getLeft() - expansion.get(0).getRight() + 1);
            result[0] = (char) ((result[0] << Byte.SIZE) | (char) BitsUtil.getBits(block, expansion.get(1).getLeft(), expansion.get(1).getLeft() - expansion.get(1).getRight() + 1));

            for (int i = 1; i < Byte.SIZE; i++) {
                result[i] = (char) BitsUtil.getBits(block, expansion.get(i + 1).getLeft(), expansion.get(i + 1).getLeft() - expansion.get(i + 1).getRight() + 1);
            }

            return result;
        }

        public static long KP(long first, long second) {
            long leftPartFirst = first >>> (Long.SIZE / 2); // +
            long rightPartFirst = (first << (Long.SIZE / 2)) >>> (Long.SIZE / 2); // +
            return (((leftPartFirst & ~second) | (rightPartFirst & second)) << (Long.SIZE / 2)) | ((rightPartFirst & ~second) | (leftPartFirst & second));
        }

        public static long P(long block) {
            return BitsUtil.bytesToLong(BitsUtil.permutation(BitsUtil.longToBytes(block, Byte.SIZE), PERMUTATION));
        }

        public static byte[] SA(char[] blocks) {
            return S(blocks, new int[]{1, 2, 1, 2, 2, 1, 2, 1});
        }

        public static byte[] SB(char[] blocks) {
            return S(blocks, new int[]{2, 2, 1, 1, 2, 2, 1, 1});
        }

        public static byte S1(char block) {
            return (byte) (GF.powMod((char) (~block ^ 0x1FFF), 3, (char) 0x2911) & 0xFF);
        }

        public static byte S2(char block) {
            return (byte) (GF.powMod((char) (~block ^ 0x7FF), 3, (char) 0xAA7) & 0xFF);
        }

        public static byte[] S(char[] blocks, int[] s) {
            byte[] result = new byte[Byte.SIZE];

            for (int i = 0; i < s.length; i++) {
                if (s[i] == 1) {
                    result[i] = S1(blocks[i]);
                } else {
                    result[i] = S2(blocks[i]);
                }
            }

            return result;
        }
    }

    static class Check {
        private Check() {}

        private static boolean checkSizeKeyInBits(int sizeKeyInBits) {
            return sizeKeyInBits == 128 || sizeKeyInBits == 192 || sizeKeyInBits == 256;
        }

        public static boolean checkKey(byte[] key, int sizeKeyInBits) {
            return checkSizeKeyInBits(sizeKeyInBits) && (key.length * 8 == sizeKeyInBits);
        }
    }
}