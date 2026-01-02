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

    private static int ceilLog2(int x) {
        // TS: Math.ceil(Math.log2(n + 1))
        // Here x is (n + 1). Need ceil(log2(x)).
        if (x <= 1) {
            return 0;
        }
        int p = 0;
        int v = 1;
        while (v < x) {
            v <<= 1;
            p++;
        }
        return p;
    }

    private static <V> void doParse(
            BigInteger prefixValue,
            int prefixLen,
            Slice slice,
            int n,
            Map<BigInteger, V> res,
            Function<Slice, V> extractor
    ) {

        // Reading label
        int lb0 = slice.loadBit() ? 1 : 0;
        int prefixLength = 0;

        BigInteger ppVal = prefixValue;
        int ppLen = prefixLen;

        if (lb0 == 0) {
            // Short label detected
            prefixLength = readUnaryLength(slice);

            // Read prefix
            for (int i = 0; i < prefixLength; i++) {
                boolean b = slice.loadBit();
                ppVal = ppVal.shiftLeft(1).or(b ? BigInteger.ONE : BigInteger.ZERO);
                ppLen++;
            }
        } else {
            int lb1 = slice.loadBit() ? 1 : 0;
            if (lb1 == 0) {
                // Long label detected
                int bitsForLen = ceilLog2(n + 1);
                long pl = slice.loadUint(bitsForLen);
                prefixLength = (int) pl;

                for (int i = 0; i < prefixLength; i++) {
                    boolean b = slice.loadBit();
                    ppVal = ppVal.shiftLeft(1).or(b ? BigInteger.ONE : BigInteger.ZERO);
                    ppLen++;
                }
            } else {
                // Same label detected
                boolean bit = slice.loadBit();
                int bitsForLen = ceilLog2(n + 1);
                long pl = slice.loadUint(bitsForLen);
                prefixLength = (int) pl;

                for (int i = 0; i < prefixLength; i++) {
                    ppVal = ppVal.shiftLeft(1).or(bit ? BigInteger.ONE : BigInteger.ZERO);
                    ppLen++;
                }
            }
        }

        if (n - prefixLength == 0) {
            // Leaf
            // TS: res.set(BigInt('0b' + pp), extractor(slice));
            res.put(ppVal, extractor.apply(slice));
        } else {
            Cell left = slice.loadRef();
            Cell right = slice.loadRef();

            // NOTE: Left and right branches implicitly contain prefixes '0' and '1'
            if (!left.isExotic()) {
                doParse(
                        ppVal.shiftLeft(1),          // + '0'
                        ppLen + 1,
                        left.beginParse(),
                        n - prefixLength - 1,
                        res,
                        extractor
                );
            }
            if (!right.isExotic()) {
                doParse(
                        ppVal.shiftLeft(1).or(BigInteger.ONE), // + '1'
                        ppLen + 1,
                        right.beginParse(),
                        n - prefixLength - 1,
                        res,
                        extractor
                );
            }
        }
    }

    public static <V> Map<BigInteger, V> parseDict(Slice sc, int keySize, Function<Slice, V> extractor) {
        Map<BigInteger, V> res = new LinkedHashMap<>();
        if (sc != null) {
            doParse(BigInteger.ZERO, 0, sc, keySize, res, extractor);
        }
        return res;
    }
}
