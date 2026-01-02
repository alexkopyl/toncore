package dev.quark.ton.core.types;

import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.Writable;

import java.util.Arrays;
import java.util.Objects;

public final class HashUpdate implements Writable {
    private final byte[] oldHash; // 32 bytes
    private final byte[] newHash; // 32 bytes

    public HashUpdate(byte[] oldHash, byte[] newHash) {
        if (oldHash == null || oldHash.length != 32) {
            throw new IllegalArgumentException("oldHash must be 32 bytes");
        }
        if (newHash == null || newHash.length != 32) {
            throw new IllegalArgumentException("newHash must be 32 bytes");
        }
        this.oldHash = oldHash.clone();
        this.newHash = newHash.clone();
    }

    public byte[] oldHash() { return oldHash.clone(); }
    public byte[] newHash() { return newHash.clone(); }

    public static HashUpdate load(Slice slice) {
        long tag = slice.loadUint(8);
        if (tag != 0x72L) {
            throw new IllegalArgumentException("Invalid HASH_UPDATE tag: " + tag);
        }
        byte[] oldHash = slice.loadBuffer(32);
        byte[] newHash = slice.loadBuffer(32);
        return new HashUpdate(oldHash, newHash);
    }

    public void store(Builder b) {
        b.storeUint(0x72, 8);
        b.storeBuffer(oldHash);
        b.storeBuffer(newHash);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HashUpdate other)) return false;
        return Arrays.equals(oldHash, other.oldHash) && Arrays.equals(newHash, other.newHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(oldHash), Arrays.hashCode(newHash));
    }

    @Override
    public void writeTo(Builder builder) {
        store(builder);
    }
}


