package dev.quark.ton.core.utils;

import java.math.BigInteger;

/**
 * Ported 1:1 from ton-core/src/utils/bitsForNumber.ts
 */
public final class BitsForNumber {

    private BitsForNumber() {}

    /**
     * @param src  number (int/long/BigInteger supported via overloads)
     * @param mode "int" or "uint"
     */
    public static int bitsForNumber(BigInteger src, Mode mode) {
        BigInteger v = src;

        // Handle negative values
        if (mode == Mode.INT) {

            // Corner case for zero or -1 value
            if (v.equals(BigInteger.ZERO) || v.equals(BigInteger.valueOf(-1))) {
                return 1;
            }

            BigInteger v2 = v.signum() > 0 ? v : v.negate();
            // binary length + 1 sign bit
            return v2.toString(2).length() + 1;

        } else if (mode == Mode.UINT) {

            if (v.signum() < 0) {
                throw new IllegalArgumentException("value is negative. Got " + src);
            }
            return v.toString(2).length();

        } else {
            throw new IllegalArgumentException("invalid mode. Got " + mode);
        }
    }

    /* ======================= overloads ======================= */

    public static int bitsForNumber(long src, Mode mode) {
        return bitsForNumber(BigInteger.valueOf(src), mode);
    }

    public static int bitsForNumber(int src, Mode mode) {
        return bitsForNumber(BigInteger.valueOf(src), mode);
    }

    /* ======================= mode ======================= */

    public enum Mode {
        INT,
        UINT
    }
}
