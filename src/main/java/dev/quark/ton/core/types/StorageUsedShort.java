package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;

// TL-B:
// storage_used_short$_ cells:(VarUInteger 7) bits:(VarUInteger 7) = StorageUsedShort;
public final class StorageUsedShort implements Writable {

    public final BigInteger cells; // VarUInteger 7
    public final BigInteger bits;  // VarUInteger 7

    public StorageUsedShort(BigInteger cells, BigInteger bits) {
        this.cells = Objects.requireNonNull(cells, "cells");
        this.bits = Objects.requireNonNull(bits, "bits");
    }

    public static StorageUsedShort load(Slice slice) {
        BigInteger cells = slice.loadVarUintBig(3);
        BigInteger bits = slice.loadVarUintBig(3);
        return new StorageUsedShort(cells, bits);
    }

    public void store(Builder builder) {
        builder.storeVarUint(cells, 3);
        builder.storeVarUint(bits, 3);
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }

    public static java.util.function.Consumer<Builder> storeStorageUsedShort(StorageUsedShort src) {
        if (src == null) throw new IllegalArgumentException("StorageUsedShort is null");
        return src::store;
    }
}
