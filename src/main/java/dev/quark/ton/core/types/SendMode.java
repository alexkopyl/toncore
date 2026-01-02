package dev.quark.ton.core.types;

public enum SendMode {
    NONE(0),
    PAY_GAS_SEPARATELY(1),
    IGNORE_ERRORS(2),
    DESTROY_ACCOUNT_IF_ZERO(32),
    CARRY_ALL_REMAINING_INCOMING_VALUE(64),
    CARRY_ALL_REMAINING_BALANCE(128);

    private final int value;

    SendMode(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static int combine(SendMode... modes) {
        int r = 0;
        for (SendMode m : modes) {
            r |= m.value;
        }
        return r;
    }

    public static boolean has(int combined, SendMode mode) {
        return (combined & mode.value) != 0;
    }
}
