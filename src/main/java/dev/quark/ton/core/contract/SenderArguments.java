package dev.quark.ton.core.contract;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.SendMode;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Port of ton-core/src/contract/Sender.ts (SenderArguments)
 *
 * Maybe<T> in TS => nullable in Java.
 */
public final class SenderArguments {

    public static final class Init {
        private final Cell code; // nullable
        private final Cell data; // nullable

        public Init(Cell code, Cell data) {
            this.code = code;
            this.data = data;
        }

        public Cell code() { return code; }
        public Cell data() { return data; }
    }

    private final BigInteger value;
    private final Address to;

    private final SendMode sendMode; // nullable
    private final Boolean bounce;    // nullable
    private final Init init;         // nullable
    private final Cell body;         // nullable

    public SenderArguments(
            BigInteger value,
            Address to,
            SendMode sendMode,
            Boolean bounce,
            Init init,
            Cell body
    ) {
        this.value = Objects.requireNonNull(value, "value");
        this.to = Objects.requireNonNull(to, "to");
        this.sendMode = sendMode;
        this.bounce = bounce;
        this.init = init;
        this.body = body;
    }

    public BigInteger value() { return value; }
    public Address to() { return to; }

    public SendMode sendMode() { return sendMode; }
    public Boolean bounce() { return bounce; }
    public Init init() { return init; }
    public Cell body() { return body; }
}
