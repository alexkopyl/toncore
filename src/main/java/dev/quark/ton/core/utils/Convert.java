package dev.quark.ton.core.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1:1 port of ton-core/src/utils/convert.ts
 *
 * toNano:
 *  - bigint -> * 1e9
 *  - number -> special formatting rules (precision guard) then parse like string
 *  - string -> sign toggling via repeated '-', parse whole/frac, frac <= 9, right-pad to 9
 *
 * fromNano:
 *  - bigint|number|string -> BigInteger -> convert to decimal with trimming trailing zeros in fractional part
 */
public final class Convert {

    private static final BigInteger NANO = BigInteger.valueOf(1_000_000_000L);
    private static final Pattern FRAC_TRIM = Pattern.compile("^([0-9]*[1-9]|0)(0*)$");

    private Convert() {}

    // ---------------------------------------------------------------------
    // toNano overloads
    // ---------------------------------------------------------------------

    public static BigInteger toNano(BigInteger src) {
        Objects.requireNonNull(src, "src");
        return src.multiply(NANO);
    }

    public static BigInteger toNano(String src) {
        Objects.requireNonNull(src, "src");
        return toNanoFromString(src);
    }

    /**
     * Port of TS behavior for number input.
     * Note: Java double cannot represent 1e64 precisely; but TS test includes 1e64 for numberCases.
     * We handle it by formatting via BigDecimal using Double.toString + log10 rule approximation.
     */
    public static BigInteger toNano(double src) {
        if (!Double.isFinite(src)) {
            throw new IllegalArgumentException("Invalid number");
        }

        // TS uses Math.log10(src) <= 6, but JS rounding often makes borderline values hit 6 exactly.
        final double EPS = 1e-12;

        double abs = Math.abs(src);
        double log10 = (abs == 0.0) ? Double.NEGATIVE_INFINITY : Math.log10(abs);

        String formatted;
        if (log10 <= 6.0 + EPS) {
            // TS: toLocaleString('en', { minimumFractionDigits: 9, useGrouping: false })
            // => effectively round to 9 fractional digits.
            formatted = new BigDecimal(Double.toString(src))
                    .setScale(9, RoundingMode.HALF_UP)
                    .toPlainString();
        } else if (src == Math.rint(src)) {
            // TS: integer -> no fraction
            formatted = new BigDecimal(Double.toString(src))
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .toPlainString();
        } else {
            throw new IllegalArgumentException("Not enough precision for a number value. Use string value instead");
        }

        return toNanoFromString(formatted);
    }

    private static BigInteger toNanoFromString(String src) {

        // Check sign with "toggle" logic (multiple leading '-')
        boolean neg = false;
        while (src.startsWith("-")) {
            neg = !neg;
            src = src.substring(1);
        }

        // Split string
        if (src.equals(".")) {
            throw new IllegalArgumentException("Invalid number");
        }

        String[] parts = src.split("\\.", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("Invalid number");
        }

        String whole = parts.length >= 1 ? parts[0] : "0";
        String frac = parts.length == 2 ? parts[1] : "0";

        if (whole == null || whole.isEmpty()) {
            whole = "0";
        }
        if (frac == null || frac.isEmpty()) {
            frac = "0";
        }

        if (frac.length() > 9) {
            throw new IllegalArgumentException("Invalid number");
        }
        while (frac.length() < 9) {
            frac = frac + "0";
        }

        BigInteger r = new BigInteger(whole).multiply(NANO).add(new BigInteger(frac));
        return neg ? r.negate() : r;
    }

    // ---------------------------------------------------------------------
    // fromNano
    // ---------------------------------------------------------------------

    public static String fromNano(BigInteger src) {
        Objects.requireNonNull(src, "src");

        BigInteger v = src;
        boolean neg = false;
        if (v.signum() < 0) {
            neg = true;
            v = v.negate();
        }

        // fraction
        BigInteger frac = v.mod(NANO);
        String fracStr = frac.toString();
        while (fracStr.length() < 9) {
            fracStr = "0" + fracStr;
        }

        Matcher m = FRAC_TRIM.matcher(fracStr);
        if (!m.matches()) {
            // theoretically shouldn't happen
            throw new IllegalStateException("Invalid fraction");
        }
        fracStr = m.group(1);

        // whole
        BigInteger whole = v.divide(NANO);
        String wholeStr = whole.toString();

        String value = wholeStr + (fracStr.equals("0") ? "" : "." + fracStr);
        return neg ? "-" + value : value;
    }

    public static String fromNano(String src) {
        Objects.requireNonNull(src, "src");
        return fromNano(new BigInteger(src));
    }

    public static String fromNano(long src) {
        return fromNano(BigInteger.valueOf(src));
    }
}
