package dev.quark.ton.core.boc;

import dev.quark.ton.core.boc.utils.PaddedBits;

import java.util.Arrays;
import java.util.Objects;

/**
 * BitString is a class that represents a bitstring in a byte array with a specified offset and length (in bits).
 *
 * Ported from ton-core/src/boc/BitString.ts
 */
public final class BitString {

    public static final BitString EMPTY = new BitString(new byte[0], 0, 0);

    // Keeping same semantics as TS: fields are internal and immutable.
    private final int offset;     // in bits
    private final int length;     // in bits
    private final byte[] data;    // backing array (expected to be not modified externally)

    /**
     * Constructing BitString from a byte array
     *
     * @param data   backing data (should NOT be modified)
     * @param offset offset in bits from the start of the buffer
     * @param length length of the bitstring in bits
     */
    public BitString(byte[] data, int offset, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length " + length + " is out of bounds");
        }
        this.length = length;
        this.data = Objects.requireNonNull(data, "data");
        this.offset = offset;
    }

    public int length() {
        return length;
    }

    /**
     * Returns the bit at the specified index
     *
     * @throws IllegalArgumentException if index is out of bounds
     */
    public boolean at(int index) {
        if (index >= length) {
            throw new IllegalArgumentException("Index " + index + " > " + length + " is out of bounds");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index " + index + " < 0 is out of bounds");
        }

        int bitPos = offset + index;
        int byteIndex = bitPos >> 3;
        int bitIndex = 7 - (bitPos & 7); // big-endian bits inside byte

        int b = data[byteIndex] & 0xFF;
        return (b & (1 << bitIndex)) != 0;
    }

    /**
     * Get a substring of the bitstring
     *
     * @throws IllegalArgumentException if out of bounds
     */
    public BitString substring(int subOffset, int subLength) {
        // Check offset
        if (subOffset > length) {
            throw new IllegalArgumentException("Offset(" + subOffset + ") > " + length + " is out of bounds");
        }
        if (subOffset < 0) {
            throw new IllegalArgumentException("Offset(" + subOffset + ") < 0 is out of bounds");
        }

        // Corner case of empty string (matches TS: returns BitString.EMPTY)
        if (subLength == 0) {
            return EMPTY;
        }

        if (subOffset + subLength > length) {
            throw new IllegalArgumentException(
                    "Offset " + subOffset + " + Length " + subLength + " > " + length + " is out of bounds"
            );
        }

        return new BitString(this.data, this.offset + subOffset, subLength);
    }

    /**
     * Try to get a byte-aligned slice without allocations.
     *
     * @return byte[] slice if aligned, null otherwise
     * @throws IllegalArgumentException if out of bounds
     */
    public byte[] subbuffer(int subOffset, int subLength) {
        // Check offset
        if (subOffset > length) {
            throw new IllegalArgumentException("Offset " + subOffset + " is out of bounds");
        }
        if (subOffset < 0) {
            throw new IllegalArgumentException("Offset " + subOffset + " is out of bounds");
        }
        if (subOffset + subLength > length) {
            throw new IllegalArgumentException("Offset + Length = " + (subOffset + subLength) + " is out of bounds");
        }

        // Alignment checks
        if (subLength % 8 != 0) {
            return null;
        }
        if (((this.offset + subOffset) % 8) != 0) {
            return null;
        }

        int start = (this.offset + subOffset) >> 3;
        int end = start + (subLength >> 3);
        return Arrays.copyOfRange(this.data, start, end);
    }

    public boolean equalsBits(BitString other) {
        if (this.length != other.length) {
            return false;
        }
        for (int i = 0; i < this.length; i++) {
            if (this.at(i) != other.at(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Canonical TON formatting, ported from TS.
     */
    @Override
    public String toString() {
        byte[] padded = PaddedBits.bitsToPaddedBuffer(this);

        if (length % 4 == 0) {
            int bytes = (int) Math.ceil(length / 8.0);
            String s = bytesToHexUpper(padded, 0, bytes);

            if (length % 8 == 0) {
                return s;
            } else {
                // remove last nibble
                return s.substring(0, s.length() - 1);
            }
        } else {
            String hex = bytesToHexUpper(padded, 0, padded.length);
            if (length % 8 <= 4) {
                return hex.substring(0, hex.length() - 1) + "_";
            } else {
                return hex + "_";
            }
        }
    }

    // ---- helpers ----

    private static String bytesToHexUpper(byte[] bytes, int off, int len) {
        char[] out = new char[len * 2];
        final char[] HEX = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < len; i++) {
            int v = bytes[off + i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
