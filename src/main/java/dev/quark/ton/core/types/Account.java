package dev.quark.ton.core.types;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.util.Objects;

public final class Account implements Writable {

    public final Address addr;
    public final StorageInfo storageStats;
    public final AccountStorage storage;

    public Account(Address addr,
                   StorageInfo storageStats,
                   AccountStorage storage) {
        this.addr = Objects.requireNonNull(addr);
        this.storageStats = Objects.requireNonNull(storageStats);
        this.storage = Objects.requireNonNull(storage);
    }

    public static Account loadAccount(Slice slice) {
        return new Account(
                slice.loadAddress(),
                StorageInfo.loadStorageInfo(slice),
                AccountStorage.loadAccountStorage(slice)
        );
    }

    public void store(Builder builder) {
        builder.storeAddress(addr);
        builder.store(StorageInfo.storeStorageInfo(storageStats));
        builder.store(storage);
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }
}
