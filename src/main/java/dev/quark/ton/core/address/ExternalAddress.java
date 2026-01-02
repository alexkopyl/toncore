package dev.quark.ton.core.address;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Analog of ton-core ExternalAddress:
 * External<bits:value>
 */
public final class ExternalAddress {

    private final BigInteger value;
    private final int bits;

    public ExternalAddress(BigInteger value, int bits) {
        this.value = Objects.requireNonNull(value, "value");
        if (bits < 0) {
            throw new IllegalArgumentException("bits must be >= 0");
        }
        this.bits = bits;
    }

    /** Type guard analog */
    public static boolean isAddress(Object src) {
        return src instanceof ExternalAddress;
    }

    public BigInteger getValue() {
        return value;
    }

    public int getBits() {
        return bits;
    }

    @Override
    public String toString() {
        return "External<" + bits + ":" + value + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExternalAddress that)) return false;
        return bits == that.bits && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, bits);
    }
}
