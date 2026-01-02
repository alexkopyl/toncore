package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * TL-B:
 * shard_ident$00 shard_pfx_bits:(#<= 60)
 *   workchain_id:int32 shard_prefix:uint64 = ShardIdent;
 */
public final class ShardIdent implements Writable {

    public final int shardPrefixBits;   // uint6
    public final int workchainId;        // int32
    public final BigInteger shardPrefix; // uint64

    public ShardIdent(int shardPrefixBits, int workchainId, BigInteger shardPrefix) {
        this.shardPrefixBits = shardPrefixBits;
        this.workchainId = workchainId;
        this.shardPrefix = Objects.requireNonNull(shardPrefix, "shardPrefix");
    }

    public static ShardIdent loadShardIdent(Slice slice) {
        long tag = slice.loadUint(2);
        if (tag != 0) {
            throw new IllegalArgumentException("Invalid ShardIdent tag: " + tag);
        }

        int shardPrefixBits = (int) slice.loadUint(6);
        int workchainId = (int) slice.loadInt(32);
        BigInteger shardPrefix = slice.loadUintBig(64);

        return new ShardIdent(shardPrefixBits, workchainId, shardPrefix);
    }

    public void store(Builder builder) {
        builder.storeUint(0, 2);
        builder.storeUint(shardPrefixBits, 6);
        builder.storeInt(workchainId, 32);
        builder.storeUint(shardPrefix, 64);
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }

    public static Consumer<Builder> storeShardIdent(ShardIdent src) {
        Objects.requireNonNull(src, "src");
        return src::store;
    }
}
