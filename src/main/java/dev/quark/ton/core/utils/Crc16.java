package dev.quark.ton.core.utils;

public final class Crc16 {

    private Crc16() {}

    /**
     * 1:1 port of crc16.ts
     * Poly: 0x1021
     * Init: 0
     * Appends two zero bytes to the message (len + 2).
     *
     * @param data input bytes
     * @return 2-byte CRC big-endian (hi, lo)
     */
    public static byte[] crc16(byte[] data) {
        final int poly = 0x1021;
        int reg = 0;

        int messageLen = (data == null ? 0 : data.length) + 2;
        for (int i = 0; i < messageLen; i++) {
            int b = 0;
            if (data != null && i < data.length) {
                b = data[i] & 0xFF;
            }

            int mask = 0x80;
            while (mask > 0) {
                reg <<= 1;
                if ((b & mask) != 0) {
                    reg += 1;
                }
                mask >>= 1;

                if (reg > 0xFFFF) {
                    reg &= 0xFFFF;
                    reg ^= poly;
                }
            }
        }

        return new byte[] {
                (byte) ((reg >>> 8) & 0xFF),
                (byte) (reg & 0xFF)
        };
    }
}
