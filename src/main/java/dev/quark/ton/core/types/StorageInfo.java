package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * TL-B:
 * storage_info$_ used:StorageUsed last_paid:uint32 due_payment:(Maybe Grams) = StorageInfo;
 */
public final class StorageInfo implements Writable {

    public final StorageUsed used;
    public final long lastPaid;          // uint32
    public final BigInteger duePayment;  // nullable (Maybe coins)

    public StorageInfo(StorageUsed used, long lastPaid, BigInteger duePayment) {
        this.used = Objects.requireNonNull(used, "used");
        this.lastPaid = lastPaid;
        this.duePayment = duePayment;
    }

    public static StorageInfo loadStorageInfo(Slice slice) {
        StorageUsed used = StorageUsed.loadStorageUsed(slice);
        long lastPaid = slice.loadUint(32);
        BigInteger duePayment = slice.loadMaybeCoins(); // должен вернуть null если absent
        return new StorageInfo(used, lastPaid, duePayment);
    }

    public void store(Builder builder) {
        builder.store(StorageUsed.storeStorageUsed(used));
        builder.storeUint(lastPaid, 32);
        builder.storeMaybeCoins(duePayment);
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }

    public static Consumer<Builder> storeStorageInfo(StorageInfo src) {
        Objects.requireNonNull(src, "src");
        return src::store;
    }
}
