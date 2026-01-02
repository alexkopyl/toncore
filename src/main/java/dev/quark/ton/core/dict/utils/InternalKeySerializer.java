package dev.quark.ton.core.dict.utils;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.utils.PaddedBits;

import java.math.BigInteger;


/**
 * 1:1 port of dict/utils/internalKeySerializer.ts
 */
public final class InternalKeySerializer {
    private InternalKeySerializer() {}

    // JS Number.MAX_SAFE_INTEGER = 2^53 - 1
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    public static String serializeInternalKey(Object value) {
        if (value instanceof Integer i) {
            long v = i.longValue();
            ensureSafeInteger(v);
            return "n:" + Long.toString(v, 10);
        } else if (value instanceof Long l) {
            ensureSafeInteger(l);
            return "n:" + Long.toString(l, 10);
        } else if (value instanceof BigInteger bi) {
            return "b:" + bi.toString(10);
        } else if (value instanceof Address a) {
            return "a:" + a.toString();
        } else if (value instanceof byte[] b) {
            return "f:" + toHexLower(b);
        } else if (value instanceof BitString bs) {
            return "B:" + bs.toString();
        } else {
            throw new IllegalArgumentException("Invalid key type");
        }
    }

    public static Object deserializeInternalKey(String value) {
        if (value == null || value.length() < 2) {
            throw new IllegalArgumentException("Invalid key type: " + value);
        }

        String k = value.substring(0, 2);
        String v = value.substring(2);

        switch (k) {
            case "n:":
                // TS parseInt => number. В Java храним как long (TS number)
                return Long.parseLong(v, 10);
            case "b:":
                return new BigInteger(v, 10);
            case "a:":
                return Address.parse(v);
            case "f:":
                return fromHex(v);
            case "B:":
                return deserializeBitString(v);
            default:
                throw new IllegalArgumentException("Invalid key type: " + k);
        }
    }

    private static void ensureSafeInteger(long v) {
        if (v > MAX_SAFE_INTEGER || v < -MAX_SAFE_INTEGER) {
            throw new IllegalArgumentException("Invalid key type: not a safe integer: " + v);
        }
    }

    private static BitString deserializeBitString(String v) {
        boolean lastDash = v.length() > 0 && v.charAt(v.length() - 1) == '_';
        boolean isPadded = lastDash || (v.length() % 2 != 0);

        if (isPadded) {
            int charLen = lastDash ? (v.length() - 1) : v.length();
            String padded = v.substring(0, charLen) + "0"; // как в TS

            // КЛЮЧЕВОЕ: если padded нечётной длины — отрезаем последний символ.
            // В TS это проходило через Buffer.from(...,'hex') и по факту этот "хвост" не влияет.
            if ((padded.length() & 1) != 0) {
                padded = padded.substring(0, padded.length() - 1);
            }

            if (!lastDash && ((charLen & 1) != 0)) {
                // odd nibble count without "_" => exact nibble length (no terminator removal)
                byte[] buf = fromHex(padded); // теперь padded уже чётной длины
                return new BitString(buf, 0, charLen << 2);
            } else {
                byte[] buf = fromHex(padded);
                return PaddedBits.paddedBufferToBits(buf);
            }
        } else {
            byte[] buf = fromHex(v);
            return new BitString(buf, 0, v.length() << 2);
        }
    }

    // ===== hex helpers (lowercase, 1:1 with Buffer.toString('hex')) =====

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static String toHexLower(byte[] data) {
        char[] out = new char[data.length * 2];
        int p = 0;
        for (byte b : data) {
            int v = b & 0xFF;
            out[p++] = HEX[v >>> 4];
            out[p++] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    private static byte[] fromHex(String hex) {
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("Invalid hex length");
        }
        int len = hex.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("Invalid hex");
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
