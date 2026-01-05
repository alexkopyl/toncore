package dev.quark.ton.core.types;

/**
 * Port of src/types/ReserveMode.ts
 *
 * TL-B (OutActionReserve):
 *  mode:(## 8)
 *
 * NOTE: this is a bitmask-like mode (can be combined), but TS lists common constants.
 */
public enum ReserveMode {
    THIS_AMOUNT(0),
    LEAVE_THIS_AMOUNT(1),
    AT_MOST_THIS_AMOUNT(2),
    LEAVE_MAX_THIS_AMOUNT(3),
    BEFORE_BALANCE_PLUS_THIS_AMOUNT(4),
    LEAVE_BBALANCE_PLUS_THIS_AMOUNT(5),
    BEFORE_BALANCE_MINUS_THIS_AMOUNT(12),
    LEAVE_BEFORE_BALANCE_MINUS_THIS_AMOUNT(13);

    private final int value;

    ReserveMode(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    /** Combine flags (if used as bitmask) */
    public static int combine(ReserveMode... modes) {
        int r = 0;
        if (modes != null) {
            for (ReserveMode m : modes) {
                if (m != null) r |= m.value;
            }
        }
        return r;
    }

    public static boolean has(int combined, ReserveMode mode) {
        return mode != null && (combined & mode.value) != 0;
    }

    public static ReserveMode fromValueOrNull(int v) {
        for (ReserveMode m : values()) {
            if (m.value() == v) return m;
        }
        return null;
    }
}
