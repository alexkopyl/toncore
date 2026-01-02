package dev.quark.ton.core.dict.utils;

import dev.quark.ton.core.boc.Slice;

public final class ReadUnaryLength {

    private ReadUnaryLength() {}

    public static int readUnaryLength(Slice slice) {
        int res = 0;
        while (slice.loadBit()) {
            res++;
        }
        return res;
    }
}
