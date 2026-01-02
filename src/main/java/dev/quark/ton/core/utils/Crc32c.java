package dev.quark.ton.core.utils;

/**
 * Ported 1:1 from ton-core/src/utils/crc32c.ts
 *
 * NOTE:
 * - No lookup tables
 * - No java.util.zip
 * - Bit-by-bit implementation
 * - Little-endian output
 */
public final class Crc32c {

    private static final int POLY = 0x82F63B78;

    private Crc32c() {}

    public static byte[] crc32c(byte[] source) {
        int crc = 0 ^ 0xFFFFFFFF;

        for (int n = 0; n < source.length; n++) {
            crc ^= (source[n] & 0xFF);

            crc = (crc & 1) != 0 ? (crc >>> 1) ^ POLY : (crc >>> 1);
            crc = (crc & 1) != 0 ? (crc >>> 1) ^ POLY : (crc >>> 1);
            crc = (crc & 1) != 0 ? (crc >>> 1) ^ POLY : (crc >>> 1);
            crc = (crc & 1) != 0 ? (crc >>> 1) ^ POLY : (crc >>> 1);
            crc = (crc & 1) != 0 ? (crc >>> 1) ^ POLY : (crc >>> 1);
            crc = (crc & 1) != 0 ? (crc >>> 1) ^ POLY : (crc >>> 1);
            crc = (crc & 1) != 0 ? (crc >>> 1) ^ POLY : (crc >>> 1);
            crc = (crc & 1) != 0 ? (crc >>> 1) ^ POLY : (crc >>> 1);
        }

        crc = crc ^ 0xFFFFFFFF;

        // Convert to little-endian (writeInt32LE)
        byte[] res = new byte[4];
        res[0] = (byte) (crc & 0xFF);
        res[1] = (byte) ((crc >>> 8) & 0xFF);
        res[2] = (byte) ((crc >>> 16) & 0xFF);
        res[3] = (byte) ((crc >>> 24) & 0xFF);

        return res;
    }
}
