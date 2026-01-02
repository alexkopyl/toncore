package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;

public final class TransactionCreditPhase implements Writable {

    public final BigInteger dueFeesCollected; // nullable
    public final CurrencyCollection credit;

    public TransactionCreditPhase(BigInteger dueFeesCollected, CurrencyCollection credit) {
        this.dueFeesCollected = dueFeesCollected; // Maybe
        this.credit = Objects.requireNonNull(credit, "credit");
    }

    public static TransactionCreditPhase load(Slice slice) {
        BigInteger dueFeesCollected = slice.loadBit() ? slice.loadCoins() : null;
        CurrencyCollection credit = CurrencyCollection.loadCurrencyCollection(slice);
        return new TransactionCreditPhase(dueFeesCollected, credit);
    }

    public void store(Builder builder) {
        if (dueFeesCollected == null) {
            builder.storeBit(false);
        } else {
            builder.storeBit(true);
            builder.storeCoins(dueFeesCollected);
        }
        builder.store(CurrencyCollection.storeCurrencyCollection(credit));
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }

    public static java.util.function.Consumer<Builder> storeTransactionCreditPhase(TransactionCreditPhase src) {
        if (src == null) throw new IllegalArgumentException("TransactionCreditPhase is null");
        return src::store;
    }
}
