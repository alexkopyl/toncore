package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;

import java.util.function.Consumer;

/**
 * TL-B: tick_tock$_ tick:Bool tock:Bool = TickTock;
 * 1:1 port of TickTock.ts
 */
public final class TickTock {
    public final boolean tick;
    public final boolean tock;

    public TickTock(boolean tick, boolean tock) {
        this.tick = tick;
        this.tock = tock;
    }

    public static TickTock loadTickTock(Slice slice) {
        return new TickTock(slice.loadBit(), slice.loadBit());
    }

    public static Consumer<Builder> storeTickTock(TickTock src) {
        return (builder) -> {
            builder.storeBit(src.tick);
            builder.storeBit(src.tock);
        };
    }
}
