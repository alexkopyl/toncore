package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Current (ton-core TS):
 * storage_used$_ cells:(VarUInteger 7) bits:(VarUInteger 7) = StorageUsed;
 *
 * Legacy vectors (e.g. tonkite):
 * storage_used$_ cells:(VarUInteger 7) bits:(VarUInteger 7) public_cells:(VarUInteger 7) = StorageUsed;
 */
public final class StorageUsed implements Writable {

    public final BigInteger cells;
    public final BigInteger bits;

    // legacy optional
    public final BigInteger publicCells; // nullable

    public StorageUsed(BigInteger cells, BigInteger bits) {
        this(cells, bits, null);
    }

    public StorageUsed(BigInteger cells, BigInteger bits, BigInteger publicCells) {
        this.cells = Objects.requireNonNull(cells, "cells");
        this.bits = Objects.requireNonNull(bits, "bits");
        this.publicCells = publicCells;
    }

    /** Current TS load (2 fields) */
    public static StorageUsed loadStorageUsed(Slice slice) {
        return new StorageUsed(
                slice.loadVarUintBig(3),
                slice.loadVarUintBig(3),
                null
        );
    }

    /** Legacy load (3 fields) */
    public static StorageUsed loadStorageUsedLegacy(Slice slice) {
        return new StorageUsed(
                slice.loadVarUintBig(3),
                slice.loadVarUintBig(3),
                slice.loadVarUintBig(3)
        );
    }

    /** Store in CURRENT format (2 fields) */
    public void storeCurrent(Builder builder) {
        builder.storeVarUint(cells, 3);
        builder.storeVarUint(bits, 3);
    }

    /** Store in LEGACY format (3 fields) */
    public void storeLegacy(Builder builder) {
        builder.storeVarUint(cells, 3);
        builder.storeVarUint(bits, 3);
        builder.storeVarUint(
                Objects.requireNonNull(publicCells, "publicCells (legacy)"),
                3
        );
    }

    @Override
    public void writeTo(Builder builder) {
        // Writable по умолчанию = CURRENT (как в TS)
        storeCurrent(builder);
    }

    /** Current store helper */
    public static Consumer<Builder> storeStorageUsed(StorageUsed src) {
        Objects.requireNonNull(src, "src");
        return src::storeCurrent;
    }

    /** Legacy store helper */
    public static Consumer<Builder> storeStorageUsedLegacy(StorageUsed src) {
        Objects.requireNonNull(src, "src");
        return src::storeLegacy;
    }
}
