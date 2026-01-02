package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * TL-B:
 * storage_used$_ cells:(VarUInteger 7) bits:(VarUInteger 7) public_cells:(VarUInteger 7) = StorageUsed;
 */
public final class StorageUsed implements Writable {

    public final BigInteger cells;
    public final BigInteger bits;
    public final BigInteger publicCells;

    public StorageUsed(BigInteger cells, BigInteger bits, BigInteger publicCells) {
        this.cells = Objects.requireNonNull(cells, "cells");
        this.bits = Objects.requireNonNull(bits, "bits");
        this.publicCells = Objects.requireNonNull(publicCells, "publicCells");
    }

    public static StorageUsed loadStorageUsed(Slice slice) {
        return new StorageUsed(
                slice.loadVarUintBig(3),
                slice.loadVarUintBig(3),
                slice.loadVarUintBig(3)
        );
    }

    public void store(Builder builder) {
        builder.storeVarUint(cells, 3);
        builder.storeVarUint(bits, 3);
        builder.storeVarUint(publicCells, 3);
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }

    public static Consumer<Builder> storeStorageUsed(StorageUsed src) {
        Objects.requireNonNull(src, "src");
        return src::store;
    }
}
