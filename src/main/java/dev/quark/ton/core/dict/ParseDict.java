package dev.quark.ton.core.dict;

import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class ParseDict {

    private ParseDict() {}

    private static int readUnaryLength(Slice slice) {
        int res = 0;
        while (slice.loadBit()) {
            res++;
        }
        return res;
    }

    // TS: Math.ceil(Math.log2(n + 1))
    private static int ceilLog2(int x) {
        if (x <= 1) return 0;
        // ceil(log2(x)) == floor(log2(x-1)) + 1
        return 32 - Integer.numberOfLeadingZeros(x - 1);
    }

    private static <V> void doParse(
            BigInteger prefixValue,
            Slice slice,
            int n,
            Map<BigInteger, V> res,
            Function<Slice, V> extractor
    ) {
        // Reading label
        int lb0 = slice.loadBit() ? 1 : 0;
        int prefixLength;
        BigInteger ppVal = prefixValue;

        if (lb0 == 0) {
            // Short label
            prefixLength = readUnaryLength(slice);
            for (int i = 0; i < prefixLength; i++) {
                boolean b = slice.loadBit();
                ppVal = ppVal.shiftLeft(1).or(b ? BigInteger.ONE : BigInteger.ZERO);
            }
        } else {
            int lb1 = slice.loadBit() ? 1 : 0;
            int lenBits = ceilLog2(n + 1);

            if (lb1 == 0) {
                // Long label detected
                prefixLength = (int) slice.loadUint(lenBits);
                for (int i = 0; i < prefixLength; i++) {
                    boolean b = slice.loadBit();
                    ppVal = ppVal.shiftLeft(1).or(b ? BigInteger.ONE : BigInteger.ZERO);
                }
            } else {
                // Same label detected
                boolean bit = slice.loadBit();                 // <-- ВАЖНО: сначала bit
                prefixLength = (int) slice.loadUint(lenBits);  // <-- потом длина
                for (int i = 0; i < prefixLength; i++) {
                    ppVal = ppVal.shiftLeft(1).or(bit ? BigInteger.ONE : BigInteger.ZERO);
                }
            }
        }

        if (n - prefixLength == 0) {
            // Leaf
            res.put(ppVal, extractor.apply(slice));
        } else {
            // Branch: must have two refs in non-exotic cells
            Cell left = slice.loadRef();
            Cell right = slice.loadRef();

            if (!left.isExotic()) {
                doParse(ppVal.shiftLeft(1), left.beginParse(), n - prefixLength - 1, res, extractor);
            }
            if (!right.isExotic()) {
                doParse(ppVal.shiftLeft(1).or(BigInteger.ONE), right.beginParse(), n - prefixLength - 1, res, extractor);
            }
        }
    }

    public static <V> Map<BigInteger, V> parseDict(Slice sc, int keySize, Function<Slice, V> extractor) {
        Map<BigInteger, V> res = new LinkedHashMap<>();
        if (sc != null) {
            doParse(BigInteger.ZERO, sc, keySize, res, extractor);
        }
        return res;
    }
}
