package dev.quark.ton.core.types;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.address.ExternalAddress;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;

import java.math.BigInteger;
import java.util.function.Consumer;

/**
 * 1:1 port of CommonMessageInfoRelaxed.ts
 */
public final class CommonMessageInfoRelaxedTLB {

    private CommonMessageInfoRelaxedTLB() {}

    public sealed interface CommonMessageInfoRelaxed
            permits Internal, ExternalOut {

        String type();
    }

    public record Internal(
            boolean ihrDisabled,
            boolean bounce,
            boolean bounced,
            Address src,                 // nullable (Maybe)
            Address dest,
            CurrencyCollection value,
            BigInteger ihrFee,
            BigInteger forwardFee,
            BigInteger createdLt,
            long createdAt
    ) implements CommonMessageInfoRelaxed {
        @Override public String type() { return "internal"; }
    }

    public record ExternalOut(
            Address src,                 // nullable (Maybe)
            ExternalAddress dest,        // nullable (Maybe)
            BigInteger createdLt,
            long createdAt
    ) implements CommonMessageInfoRelaxed {
        @Override public String type() { return "external-out"; }
    }

    public static CommonMessageInfoRelaxed loadCommonMessageInfoRelaxed(Slice slice) {

        // Internal message
        if (!slice.loadBit()) {

            boolean ihrDisabled = slice.loadBit();
            boolean bounce = slice.loadBit();
            boolean bounced = slice.loadBit();
            Address src = slice.loadMaybeAddress();
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

        // External In is not allowed
        if (!slice.loadBit()) {
            throw new IllegalStateException("External In message is not possible for CommonMessageInfoRelaxed");
        }

        // External Out message
        Address src = slice.loadMaybeAddress();
        ExternalAddress dest = slice.loadMaybeExternalAddress();
        BigInteger createdLt = slice.loadUintBig(64);
        long createdAt = slice.loadUint(32);

        return new ExternalOut(src, dest, createdLt, createdAt);
    }

    public static Consumer<Builder> storeCommonMessageInfoRelaxed(CommonMessageInfoRelaxed source) {
        return (builder) -> {
            if (source instanceof Internal s) {
                builder.storeBit(false);
                builder.storeBit(s.ihrDisabled());
                builder.storeBit(s.bounce());
                builder.storeBit(s.bounced());
                builder.storeAddress(s.src());       // TS: storeAddress(maybeAddress)
                builder.storeAddress(s.dest());
                builder.store(CurrencyCollection.storeCurrencyCollection(s.value()));
                builder.storeCoins(s.ihrFee());
                builder.storeCoins(s.forwardFee());
                builder.storeUint(s.createdLt(), 64);
                builder.storeUint(s.createdAt(), 32);

            } else if (source instanceof ExternalOut s) {
                builder.storeBit(true);
                builder.storeBit(true);
                builder.storeAddress(s.src());       // TS: storeAddress(maybeAddress)
                builder.storeAddress(s.dest());      // TS: storeAddress(maybeExternal)
                builder.storeUint(s.createdLt(), 64);
                builder.storeUint(s.createdAt(), 32);

            } else {
                throw new IllegalArgumentException("Unknown CommonMessageInfoRelaxed type");
            }
        };
    }
}
