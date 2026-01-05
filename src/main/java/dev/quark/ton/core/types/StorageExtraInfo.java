package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * TL-B:
 * storage_extra_none$000 = StorageExtraInfo;
 * storage_extra_info$001 dict_hash:uint256 = StorageExtraInfo;
 *
 * Port of StorageExtraInfo.ts
 */
public final class StorageExtraInfo {

    private final BigInteger dictHash; // uint256

    public StorageExtraInfo(BigInteger dictHash) {
        this.dictHash = Objects.requireNonNull(dictHash, "dictHash");
    }

    public BigInteger dictHash() {
        return dictHash;
    }

    /**
     * TS: loadStorageExtraInfo(slice): StorageExtraInfo | null
     */
    public static StorageExtraInfo loadStorageExtraInfo(Slice slice) {
        long header = slice.loadUint(3);
        if (header == 0) {
            return null;
        }
        if (header == 1) {
            return new StorageExtraInfo(slice.loadUintBig(256));
        }
        throw new IllegalArgumentException("Invalid storage extra info header: " + header);
    }

    /**
     * TS: storeStorageExtraInfo(src: StorageExtraInfo | null) => (builder) => { ... }
     */
    public static Consumer<Builder> storeStorageExtraInfo(StorageExtraInfo src) {
        return (builder) -> {
            if (src == null) {
                builder.storeUint(0, 3);
            } else {
                builder.storeUint(1, 3);
                builder.storeUint(src.dictHash, 256);
            }
        };
    }
}
