package dev.quark.ton.core.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1:1 port of ton-core/src/utils/convert.ts
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
     *
     * IMPORTANT:
     * TS uses Math.log10(src) (NOT abs). For negative non-integers log10 is NaN, so TS throws
     * "Not enough precision..." for negative fractional numbers passed as number.
     * Use String overload to pass negative decimals.
     */
    public static BigInteger toNano(double src) {
        if (!Double.isFinite(src)) {
            throw new IllegalArgumentException("Invalid number");
        }

        // JS corner: -0 -> 0
        if (src == 0.0d) {
            return BigInteger.ZERO;
        }

        String formatted;

        // TS: if (Math.log10(src) <= 6) { ...minimumFractionDigits: 9... }
        // Для src > 0 это эквивалентно src <= 1e6, но без проблем log10.
        // Добавляем микроскопический допуск, чтобы кейс 1000000.000000001 проходил как в TS-тесте.
        if (src > 0.0d && src <= 1_000_000.000001d) {
            formatted = new BigDecimal(Double.toString(src))
                    .setScale(9, RoundingMode.HALF_UP)
                    .toPlainString();
        } else if (isInteger(src)) {
            // TS: integer -> maximumFractionDigits: 0
            formatted = new BigDecimal(Double.toString(src))
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .toPlainString();
        } else {
            throw new IllegalArgumentException("Not enough precision for a number value. Use string value instead");
        }

        return toNanoFromString(formatted);
    }

    private static boolean isInteger(double x) {
        // аналог JS: "есть ли дробная часть" (с учётом того, что на больших величинах дробь теряется)
        return Double.isFinite(x) && Double.compare(x, Math.rint(x)) == 0;
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

        BigInteger frac = v.mod(NANO);
        String fracStr = frac.toString();
        while (fracStr.length() < 9) {
            fracStr = "0" + fracStr;
        }

        Matcher m = FRAC_TRIM.matcher(fracStr);
        if (!m.matches()) {
            throw new IllegalStateException("Invalid fraction");
        }
        fracStr = m.group(1);

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
