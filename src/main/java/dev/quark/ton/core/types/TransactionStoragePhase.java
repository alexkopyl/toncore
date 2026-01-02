package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;

public final class TransactionStoragePhase implements Writable {

    public final BigInteger storageFeesCollected;
    public final BigInteger storageFeesDue; // nullable
    public final AccountStatusChange statusChange;

    public TransactionStoragePhase(BigInteger storageFeesCollected,
                                   BigInteger storageFeesDue,
                                   AccountStatusChange statusChange) {
        this.storageFeesCollected = Objects.requireNonNull(storageFeesCollected, "storageFeesCollected");
        this.storageFeesDue = storageFeesDue; // Maybe
        this.statusChange = Objects.requireNonNull(statusChange, "statusChange");
    }

    public static TransactionStoragePhase load(Slice slice) {
        BigInteger storageFeesCollected = slice.loadCoins();
        BigInteger storageFeesDue = slice.loadBit() ? slice.loadCoins() : null;
        AccountStatusChange statusChange = AccountStatusChange.load(slice);
        return new TransactionStoragePhase(storageFeesCollected, storageFeesDue, statusChange);
    }

    public void store(Builder builder) {
        builder.storeCoins(storageFeesCollected);
        if (storageFeesDue == null) {
            builder.storeBit(false);
        } else {
            builder.storeBit(true);
            builder.storeCoins(storageFeesDue);
        }
        builder.store(AccountStatusChange.storeAccountStatusChange(statusChange));
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }

    public static java.util.function.Consumer<Builder> storeTransactionsStoragePhase(TransactionStoragePhase src) {
        if (src == null) throw new IllegalArgumentException("TransactionStoragePhase is null");
        return src::store;
    }
}
