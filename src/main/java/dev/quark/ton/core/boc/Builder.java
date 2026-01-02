package dev.quark.ton.core.boc;

import dev.quark.ton.core.boc.utils.Strings;
import dev.quark.ton.core.dict.Dictionary;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builder for Cells
 *
 * Ported from ton-core/src/boc/Builder.ts
 */
public final class Builder {

    private final BitBuilder bits;
    private final List<Cell> refs;

    public Builder() {
        this.bits = new BitBuilder();
        this.refs = new ArrayList<>();
    }

    /**
     * Start building a cell (TS: beginCell()).
     */
    public static Builder beginCell() {
        return new Builder();
    }

    // --- getters (TS get bits/refs/available*) ---

    public int bits() {
        return bits.length();
    }

    public int refs() {
        return refs.size();
    }

    public int availableBits() {
        return 1023 - bits();
    }

    public int availableRefs() {
        return 4 - refs();
    }

    // --- bits primitives ---

    public Builder storeBit(boolean value) {
        bits.writeBit(value);
        return this;
    }

    /** TS compatibility: positive => 1; else => 0 */
    public Builder storeBit(int value) {
        bits.writeBit(value);
        return this;
    }

    public Builder storeBits(BitString src) {
        bits.writeBits(Objects.requireNonNull(src, "src"));
        return this;
    }

    // --- buffers ---

    public Builder storeBuffer(byte[] src) {
        Objects.requireNonNull(src, "src");
        bits.writeBuffer(src);
        return this;
    }

    public Builder storeBuffer(byte[] src, Integer bytesLen) {
        Objects.requireNonNull(src, "src");
        if (bytesLen != null && src.length != bytesLen) {
            throw new IllegalArgumentException("Buffer length " + src.length + " is not equal to " + bytesLen);
        }
        bits.writeBuffer(src);
        return this;
    }

    public Builder storeMaybeBuffer(byte[] src) {
        return storeMaybeBuffer(src, null);
    }

    public Builder storeMaybeBuffer(byte[] src, Integer bytesLen) {
        if (src != null) {
            storeBit(1);
            storeBuffer(src, bytesLen);
        } else {
            storeBit(0);
        }
        return this;
    }

    // --- uint/int ---

    public Builder storeUint(long value, int bits) {
        this.bits.writeUint(value, bits);
        return this;
    }

    public Builder storeUint(BigInteger value, int bits) {
        this.bits.writeUint(value, bits);
        return this;
    }

    public Builder storeMaybeUint(Long value, int bits) {
        if (value != null) {
            storeBit(1);
            storeUint(value, bits);
        } else {
            storeBit(0);
        }
        return this;
    }

    public Builder storeMaybeUint(BigInteger value, int bits) {
        if (value != null) {
            storeBit(1);
            storeUint(value, bits);
        } else {
            storeBit(0);
        }
        return this;
    }

    public Builder storeInt(long value, int bits) {
        this.bits.writeInt(value, bits);
        return this;
    }

    public Builder storeInt(BigInteger value, int bits) {
        this.bits.writeInt(value, bits);
        return this;
    }

    public Builder storeMaybeInt(Long value, int bits) {
        if (value != null) {
            storeBit(1);
            storeInt(value, bits);
        } else {
            storeBit(0);
        }
        return this;
    }

    public Builder storeMaybeInt(BigInteger value, int bits) {
        if (value != null) {
            storeBit(1);
            storeInt(value, bits);
        } else {
            storeBit(0);
        }
        return this;
    }

    // --- varuint/varint ---

    public Builder storeVarUint(long value, int headerBits) {
        this.bits.writeVarUint(value, headerBits);
        return this;
    }

    public Builder storeVarUint(BigInteger value, int headerBits) {
        this.bits.writeVarUint(value, headerBits);
        return this;
    }

    public Builder storeMaybeVarUint(Long value, int headerBits) {
        if (value != null) {
            storeBit(1);
            storeVarUint(value, headerBits);
        } else {
            storeBit(0);
        }
        return this;
    }

    public Builder storeMaybeVarUint(BigInteger value, int headerBits) {
        if (value != null) {
            storeBit(1);
            storeVarUint(value, headerBits);
        } else {
            storeBit(0);
        }
        return this;
    }

    public Builder storeVarInt(long value, int headerBits) {
        this.bits.writeVarInt(value, headerBits);
        return this;
    }

    public Builder storeVarInt(BigInteger value, int headerBits) {
        this.bits.writeVarInt(value, headerBits);
        return this;
    }

    public Builder storeMaybeVarInt(Long value, int headerBits) {
        if (value != null) {
            storeBit(1);
            storeVarInt(value, headerBits);
        } else {
            storeBit(0);
        }
        return this;
    }

    public Builder storeMaybeVarInt(BigInteger value, int headerBits) {
        if (value != null) {
            storeBit(1);
            storeVarInt(value, headerBits);
        } else {
            storeBit(0);
        }
        return this;
    }

    // --- coins ---

    public Builder storeCoins(long amount) {
        this.bits.writeCoins(amount);
        return this;
    }

    public Builder storeCoins(BigInteger amount) {
        this.bits.writeCoins(amount);
        return this;
    }

    public Builder storeMaybeCoins(Long amount) {
        if (amount != null) {
            storeBit(1);
            storeCoins(amount);
        } else {
            storeBit(0);
        }
        return this;
    }

    public Builder storeMaybeCoins(BigInteger amount) {
        if (amount != null) {
            storeBit(1);
            storeCoins(amount);
        } else {
            storeBit(0);
        }
        return this;
    }

    // --- address ---

    /**
     * Store address (Address | ExternalAddress | null).
     * TS: storeAddress(address: Maybe<Address | ExternalAddress>)
     */
    public Builder storeAddress(Object address) {
        this.bits.writeAddress(address);
        return this;
    }

    // --- refs ---

    public Builder storeRef(Cell cell) {
        if (refs.size() >= 4) {
            throw new IllegalStateException("Too many references");
        }
        refs.add(Objects.requireNonNull(cell, "cell"));
        return this;
    }

    public Builder storeRef(Builder builder) {
        if (refs.size() >= 4) {
            throw new IllegalStateException("Too many references");
        }
        refs.add(Objects.requireNonNull(builder, "builder").endCell());
        return this;
    }

    public Builder storeMaybeRef(Object cellOrBuilder) {
        if (cellOrBuilder != null) {
            storeBit(1);
            if (cellOrBuilder instanceof Cell c) {
                storeRef(c);
            } else if (cellOrBuilder instanceof Builder b) {
                storeRef(b);
            } else {
                throw new IllegalArgumentException("Invalid argument");
            }
        } else {
            storeBit(0);
        }
        return this;
    }

    // --- slice/builder copy ---

    public Builder storeSlice(Slice src) {
        Slice c = Objects.requireNonNull(src, "src").cloneSlice();

        if (c.remainingBits() > 0) {
            storeBits(c.loadBits(c.remainingBits()));
        }
        while (c.remainingRefs() > 0) {
            storeRef(c.loadRef());
        }
        return this;
    }

    public Builder storeMaybeSlice(Slice src) {
        if (src != null) {
            storeBit(1);
            storeSlice(src);
        } else {
            storeBit(0);
        }
        return this;
    }

    public Builder storeBuilder(Builder src) {
        return storeSlice(Objects.requireNonNull(src, "src").endCell().beginParse());
    }

    public Builder storeMaybeBuilder(Builder src) {
        if (src != null) {
            storeBit(1);
            storeBuilder(src);
        } else {
            storeBit(0);
        }
        return this;
    }

    // --- writable / store(writer) ---

    /**
     * TS: storeWritable(writer: ((builder: Builder) => void) | Writable)
     */
    public Builder storeWritable(Object writer) {
        if (writer instanceof Writable w) {
            w.writeTo(this);
        } else if (writer instanceof Consumer<?> c) {
            @SuppressWarnings("unchecked")
            Consumer<Builder> cb = (Consumer<Builder>) c;
            cb.accept(this);
        } else {
            throw new IllegalArgumentException("Invalid writer");
        }
        return this;
    }

    public Builder storeMaybeWritable(Object writer) {
        if (writer != null) {
            storeBit(1);
            storeWritable(writer);
        } else {
            storeBit(0);
        }
        return this;
    }

    /**
     * Alias of storeWritable, like in TS: store(...)
     */

    public Builder store(Writable w) {
        Objects.requireNonNull(w, "w");
        w.writeTo(this);
        return this;
    }

    public Builder store(Consumer<Builder> c) {
        Objects.requireNonNull(c, "c");
        c.accept(this);
        return this;
    }

    public Builder store(Object writer) {
        return storeWritable(writer);
    }

    // --- strings ---

    public Builder storeStringTail(String src) {
        Strings.writeString(Objects.requireNonNull(src, "src"), this);
        return this;
    }

    public Builder storeMaybeStringTail(String src) {
        if (src != null) {
            storeBit(1);
            Strings.writeString(src, this);
        } else {
            storeBit(0);
        }
        return this;
    }

    public Builder storeStringRefTail(String src) {
        storeRef(Builder.beginCell().storeStringTail(Objects.requireNonNull(src, "src")));
        return this;
    }

    public Builder storeMaybeStringRefTail(String src) {
        if (src != null) {
            storeBit(1);
            storeStringRefTail(src);
        } else {
            storeBit(0);
        }
        return this;
    }

    // --- dictionaries ---

    public <K, V> Builder storeDict(Dictionary<K, V> dict, Dictionary.DictionaryKey<K> key, Dictionary.DictionaryValue<V> value) {
        if (dict != null) {
            dict.store(this, key, value);
        } else {
            storeBit(0);
        }
        return this;
    }

    public <K, V> Builder storeDictDirect(Dictionary<K, V> dict, Dictionary.DictionaryKey<K> key, Dictionary.DictionaryValue<V> value) {
        Objects.requireNonNull(dict, "dict");
        dict.storeDirect(this, key, value);
        return this;
    }

    // --- finalize ---

    public Cell endCell() {
        Cell.Options o = new Cell.Options();
        o.bits = bits.build();
        o.refs = refs;      // Cell внутри сделает копию и завернёт в unmodifiableList
        o.exotic = false;   // обычная ячейка, как в ton-core
        return new Cell(o);
    }

    public Cell endCell(Boolean exotic) {
        Cell.Options o = new Cell.Options();
        o.bits = bits.build();
        o.refs = refs;
        o.exotic = exotic != null && exotic;
        return new Cell(o);
    }

    public Cell endCellExotic() {
        Cell.Options o = new Cell.Options();
        o.bits = bits.build();
        o.refs = refs;      // Cell внутри сделает копию и завернёт в unmodifiableList
        o.exotic = true;    // <- главное отличие
        return new Cell(o);
    }


    public Cell asCell() {
        return endCell();
    }

    public Slice asSlice() {
        return endCell().beginParse();
    }
}
