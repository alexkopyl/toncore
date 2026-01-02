package dev.quark.ton.core.types;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.address.ExternalAddress;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 1:1 port of CommonMessageInfo.ts
 */
public final class CommonMessageInfoTLB {

    private CommonMessageInfoTLB() {}

    // TS union:
    // CommonMessageInfo = Internal | ExternalOut | ExternalIn
    public sealed interface CommonMessageInfo
            permits Internal, ExternalIn, ExternalOut {

        String type();
    }

    public record Internal(
            boolean ihrDisabled,
            boolean bounce,
            boolean bounced,
            Address src,
            Address dest,
            CurrencyCollection value,
            BigInteger ihrFee,
            BigInteger forwardFee,
            BigInteger createdLt,
            long createdAt
    ) implements CommonMessageInfo {
        @Override public String type() { return "internal"; }
    }

    public record ExternalIn(
            ExternalAddress src,   // nullable (Maybe)
            Address dest,
            BigInteger importFee
    ) implements CommonMessageInfo {
        @Override public String type() { return "external-in"; }
    }

    public record ExternalOut(
            Address src,
            ExternalAddress dest,  // nullable (Maybe)
            BigInteger createdLt,
            long createdAt
    ) implements CommonMessageInfo {
        @Override public String type() { return "external-out"; }
    }

    public static CommonMessageInfo loadCommonMessageInfo(Slice slice) {

        // Internal message: first bit = 0
        if (!slice.loadBit()) {

            boolean ihrDisabled = slice.loadBit();
            boolean bounce = slice.loadBit();
            boolean bounced = slice.loadBit();
            Address src = slice.loadAddress();
            Address dest = slice.loadAddress();

            CurrencyCollection value = CurrencyCollection.loadCurrencyCollection(slice);
            BigInteger ihrFee = slice.loadCoins();
            BigInteger forwardFee = slice.loadCoins();
            BigInteger createdLt = slice.loadUintBig(64);
            long createdAt = slice.loadUint(32);

            return new Internal(
                    ihrDisabled, bounce, bounced,
                    src, dest,
                    value,
                    ihrFee, forwardFee,
                    createdLt, createdAt
            );
        }

        // External In message: second bit = 0
        if (!slice.loadBit()) {
            ExternalAddress src = slice.loadMaybeExternalAddress();
            Address dest = Objects.requireNonNull(slice.loadAddress());
            BigInteger importFee = slice.loadCoins();
            return new ExternalIn(src, dest, importFee);
        }

        // External Out message: second bit = 1
        Address src = Objects.requireNonNull(slice.loadAddress());
        ExternalAddress dest = slice.loadMaybeExternalAddress();
        BigInteger createdLt = slice.loadUintBig(64);
        long createdAt = slice.loadUint(32);

        return new ExternalOut(src, dest, createdLt, createdAt);
    }

    public static Consumer<Builder> storeCommonMessageInfo(CommonMessageInfo source) {
        return (builder) -> {
            if (source instanceof Internal s) {
                builder.storeBit(false);
                builder.storeBit(s.ihrDisabled());
                builder.storeBit(s.bounce());
                builder.storeBit(s.bounced());
                builder.storeAddress(s.src());
                builder.storeAddress(s.dest());
                builder.store(CurrencyCollection.storeCurrencyCollection(s.value()));
                builder.storeCoins(s.ihrFee());
                builder.storeCoins(s.forwardFee());
                builder.storeUint(s.createdLt(), 64);
                builder.storeUint(s.createdAt(), 32);

            } else if (source instanceof ExternalIn s) {
                builder.storeBit(true);
                builder.storeBit(false);
                builder.storeAddress(s.src());   // TS: storeAddress(maybeExternal)
                builder.storeAddress(s.dest());
                builder.storeCoins(s.importFee());

            } else if (source instanceof ExternalOut s) {
                builder.storeBit(true);
                builder.storeBit(true);
                builder.storeAddress(s.src());
                builder.storeAddress(s.dest());  // TS: storeAddress(maybeExternal)
                builder.storeUint(s.createdLt(), 64);
                builder.storeUint(s.createdAt(), 32);

            } else {
                throw new IllegalArgumentException("Unknown CommonMessageInfo type");
            }
        };
    }
}
