package dev.quark.ton.core.boc.utils;

import dev.quark.ton.core.boc.BitBuilder;
import dev.quark.ton.core.boc.BitString;

public final class PaddedBits {

    private PaddedBits() {}

    /** 1:1 port of bitsToPaddedBuffer(bits: BitString) */
    public static byte[] bitsToPaddedBuffer(BitString bits) {

        // Create builder
        int totalBits = ((bits.length() + 7) / 8) * 8; // ceil(len/8)*8
        BitBuilder builder = new BitBuilder(totalBits);
        builder.writeBits(bits);

        // Apply padding
        int padding = totalBits - bits.length();
        for (int i = 0; i < padding; i++) {
            if (i == 0) {
                builder.writeBit(true);   // 1
            } else {
                builder.writeBit(false);  // 0
            }
        }

        return builder.buffer(); // byte[]
    }

    /** 1:1 port of paddedBufferToBits(buff: Buffer) */
    public static BitString paddedBufferToBits(byte[] buff) {
        int bitLen = 0;

        // Finding rightmost non-zero byte in the buffer
        for (int i = buff.length - 1; i >= 0; i--) {
            int testByte = buff[i] & 0xFF;
            if (testByte != 0) {

                // Looking for a rightmost set padding bit
                int bitPos = Integer.lowestOneBit(testByte); // testByte & -testByte

                // TS:
                // if((bitPos & 1) == 0) { bitPos = Math.log2(bitPos) + 1; }
                // В Java удобнее: trailingZeros(bitPos)+1
                if ((bitPos & 1) == 0) {
                    bitPos = Integer.numberOfTrailingZeros(bitPos) + 1;
                }

                if (i > 0) {
                    bitLen = i << 3; // Number of full bytes * 8
                }

                bitLen += 8 - bitPos;
                break;
            }
        }

        return new BitString(buff, 0, bitLen);
    }
}
