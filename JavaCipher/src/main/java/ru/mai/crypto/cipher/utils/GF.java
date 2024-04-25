package ru.mai.crypto.cipher.utils;

public class GF {
    private GF() {}

    public static int multiplication(char firstPolynomial, char secondPolynomial) {
        int resMultiplication = 0;

        for (int i = 0; i < Character.SIZE; i++) {
            if (((secondPolynomial >> i) & 1) == 1) {
                resMultiplication ^= BitsUtil.convertCharToInt(firstPolynomial) << i;
            }
        }

        return resMultiplication;
    }

    public static char multiplicationModulo(char firstPolynomial, char secondPolynomial, char modulo) {
        return mod(multiplication(firstPolynomial, secondPolynomial), modulo);
    }

    public static char powMod(char polynomial, int power, char modulo) {
        char result = 1;

        while (power != 0) {
            if ((power & 1) == 1) {
                result = multiplicationModulo(result, polynomial, modulo);
            }

            polynomial = multiplicationModulo(polynomial, polynomial, modulo);
            power >>= 1;
        }

        return result;
    }

    public static char mod(long polynomial, char modulo) {
        byte sizeModulo = getSize(modulo);
        int leftShiftValue = Long.SIZE;

        while (leftShiftValue != 0) {
            if (((polynomial >> leftShiftValue) & 1) == 1) {
                polynomial ^= (char) (modulo << (leftShiftValue - sizeModulo + 1));
            }

            leftShiftValue--;
        }

        return (char) polynomial;
    }

    public static byte getSize(char polynomial) {
        byte size = Character.SIZE;

        while (((polynomial >> (size - 1)) & 1) == 0) {
            size--;
        }

        return size;
    }
}
