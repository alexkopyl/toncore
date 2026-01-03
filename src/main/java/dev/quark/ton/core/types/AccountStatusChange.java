package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;

public enum AccountStatusChange {
    UNCHANGED,
    FROZEN,
    DELETED;

    public static AccountStatusChange load(Slice slice) {
        if (!slice.loadBit()) {
            return UNCHANGED;
        }
        // first bit was 1
        if (slice.loadBit()) {
            return DELETED; // 11
        } else {
            return FROZEN;  // 10
        }
    }

    public static java.util.function.Consumer<Builder> storeAccountStatusChange(AccountStatusChange src) {
        if (src == null) throw new IllegalArgumentException("AccountStatusChange is null");
        return (builder) -> {
            switch (src) {
                case UNCHANGED -> builder.storeBit(0);
                case FROZEN -> {
                    builder.storeBit(1);
                    builder.storeBit(0);
                }
                case DELETED -> {
                    builder.storeBit(1);
                    builder.storeBit(1);
                }
                default -> throw new IllegalArgumentException("Invalid account status change: " + src);
            }
        };
    }
}
