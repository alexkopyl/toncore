package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;

public enum AccountStatus {
    UNINITIALIZED, // 00
    FROZEN,        // 01
    ACTIVE,        // 10
    NON_EXISTING;  // 11

    public static AccountStatus loadAccountStatus(Slice slice) {
        int status = (int) slice.loadUint(2);
        return switch (status) {
            case 0x00 -> UNINITIALIZED;
            case 0x01 -> FROZEN;
            case 0x02 -> ACTIVE;
            case 0x03 -> NON_EXISTING;
            default -> throw new IllegalArgumentException("Invalid AccountStatus: " + status);
        };
    }

    public static java.util.function.Consumer<Builder> storeAccountStatus(AccountStatus src) {
        return builder -> {
            switch (src) {
                case UNINITIALIZED -> builder.storeUint(0x00, 2);
                case FROZEN -> builder.storeUint(0x01, 2);
                case ACTIVE -> builder.storeUint(0x02, 2);
                case NON_EXISTING -> builder.storeUint(0x03, 2);
            }
        };
    }
}
