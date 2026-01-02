package dev.quark.ton.core.utils;

import java.util.Arrays;

public final class Base32 {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz234567";

    private Base32() {
    }

    public static String base32Encode(byte[] buffer) {
        int length = buffer.length;
        int bits = 0;
        int value = 0;
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < length; i++) {
            value = (value << 8) | (buffer[i] & 0xFF);
            bits += 8;
            while (bits >= 5) {
                output.append(ALPHABET.charAt((value >>> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            output.append(ALPHABET.charAt((value << (5 - bits)) & 31));
        }
        return output.toString();
    }

    public static byte[] base32Decode(String input) {
        String cleaned = input.toLowerCase();
        int length = cleaned.length();
        int bits = 0;
        int value = 0;
        int index = 0;
        byte[] output = new byte[(length * 5) / 8]; // floor, как в TS

        for (int i = 0; i < length; i++) {
            value = (value << 5) | readChar(cleaned.charAt(i));
            bits += 5;
            if (bits >= 8) {
                output[index++] = (byte) ((value >>> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        // index должен совпасть с output.length, но на всякий случай:
        return index == output.length ? output : Arrays.copyOf(output, index);
    }

    private static int readChar(char ch) {
        int idx = ALPHABET.indexOf(ch);
        if (idx == -1) {
            throw new IllegalArgumentException("Invalid character found: " + ch);
        }
        return idx;
    }
}
