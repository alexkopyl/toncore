package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;

public enum ComputeSkipReason {
    NO_STATE,  // 0
    BAD_STATE, // 1
    NO_GAS;    // 2

    public static ComputeSkipReason load(Slice slice) {
        int reason = (int) slice.loadUint(2);
        return switch (reason) {
            case 0x00 -> NO_STATE;
            case 0x01 -> BAD_STATE;
            case 0x02 -> NO_GAS;
            default -> throw new IllegalArgumentException("Unknown ComputeSkipReason: " + reason);
        };
    }

    public static java.util.function.Consumer<Builder> storeComputeSkipReason(ComputeSkipReason src) {
        if (src == null) throw new IllegalArgumentException("ComputeSkipReason is null");
        return (builder) -> {
            switch (src) {
                case NO_STATE -> builder.storeUint(0x00, 2);
                case BAD_STATE -> builder.storeUint(0x01, 2);
                case NO_GAS -> builder.storeUint(0x02, 2);
                default -> throw new IllegalArgumentException("Unknown ComputeSkipReason: " + src);
            }
        };
    }
}
