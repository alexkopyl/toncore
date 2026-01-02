package dev.quark.ton.core.boc;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.address.ExternalAddress;
import dev.quark.ton.core.boc.utils.Strings;
import dev.quark.ton.core.dict.Dictionary;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Slice is a class that allows to read cell data
 *
 * Ported 1:1 from ton-core/src/boc/Slice.ts
 */
public final class Slice {

    private final BitReader reader;
    private final List<Cell> refs;
    private int refsOffset;

    public Slice(BitReader reader, List<Cell> refs) {
        this.reader = reader.cloneReader();
        this.refs = new ArrayList<>(refs);
        this.refsOffset = 0;
    }

    /* ==================== getters ==================== */

    public int remainingBits() {
        return reader.remaining();
    }

    public int offsetBits() {
        return reader.offset();
    }

    public int remainingRefs() {
        return refs.size() - refsOffset;
    }

    public int offsetRefs() {
        return refsOffset;
    }

    /* ==================== navigation ==================== */

    public Slice skip(int bits) {
        reader.skip(bits);
        return this;
    }

    /* ==================== bits ==================== */

    public boolean loadBit() {
        return reader.loadBit();
    }

    public boolean preloadBit() {
        return reader.preloadBit();
    }

    public boolean loadBoolean() {
        return loadBit();
    }

    public Boolean loadMaybeBoolean() {
        return loadBit() ? loadBoolean() : null;
    }

    public BitString loadBits(int bits) {
        return reader.loadBits(bits);
    }

    public BitString preloadBits(int bits) {
        return reader.preloadBits(bits);
    }

    /* ==================== uint / int ==================== */

    public long loadUint(int bits) {
        return reader.loadUint(bits);
    }

    public BigInteger loadUintBig(int bits) {
        return reader.loadUintBig(bits);
    }

    public long preloadUint(int bits) {
        return reader.preloadUint(bits);
    }

    public BigInteger preloadUintBig(int bits) {
        return reader.preloadUintBig(bits);
    }

    public Long loadMaybeUint(int bits) {
        return loadBit() ? loadUint(bits) : null;
    }

    public BigInteger loadMaybeUintBig(int bits) {
        return loadBit() ? loadUintBig(bits) : null;
    }

    public long loadInt(int bits) {
        return reader.loadInt(bits);
    }

    public BigInteger loadIntBig(int bits) {
        return reader.loadIntBig(bits);
    }

    public long preloadInt(int bits) {
        return reader.preloadInt(bits);
    }

    public BigInteger preloadIntBig(int bits) {
        return reader.preloadIntBig(bits);
    }

    public Long loadMaybeInt(int bits) {
        return loadBit() ? loadInt(bits) : null;
    }

    public BigInteger loadMaybeIntBig(int bits) {
        return loadBit() ? loadIntBig(bits) : null;
    }

    /* ==================== varuint / varint ==================== */

    public long loadVarUint(int bits) {
        return reader.loadVarUint(bits);
    }

    public BigInteger loadVarUintBig(int bits) {
        return reader.loadVarUintBig(bits);
    }

    public long preloadVarUint(int bits) {
        return reader.preloadVarUint(bits);
    }

    public BigInteger preloadVarUintBig(int bits) {
        return reader.preloadVarUintBig(bits);
    }

    public long loadVarInt(int bits) {
        return reader.loadVarInt(bits);
    }

    public BigInteger loadVarIntBig(int bits) {
        return reader.loadVarIntBig(bits);
    }

    public long preloadVarInt(int bits) {
        return reader.preloadVarInt(bits);
    }

    public BigInteger preloadVarIntBig(int bits) {
        return reader.preloadVarIntBig(bits);
    }

    /* ==================== coins ==================== */

    public BigInteger loadCoins() {
        return reader.loadCoins();
    }

    public BigInteger preloadCoins() {
        return reader.preloadCoins();
    }

    public BigInteger loadMaybeCoins() {
        return reader.loadBit() ? reader.loadCoins() : null;
    }

    /* ==================== addresses ==================== */

    public Address loadAddress() {
        return reader.loadAddress();
    }

    public Address loadMaybeAddress() {
        return reader.loadMaybeAddress();
    }

    public ExternalAddress loadExternalAddress() {
        return reader.loadExternalAddress();
    }

    public ExternalAddress loadMaybeExternalAddress() {
        return reader.loadMaybeExternalAddress();
    }

    public Object loadAddressAny() {
        return reader.loadAddressAny();
    }

    /* ==================== refs ==================== */

    public Cell loadRef() {
        if (refsOffset >= refs.size()) {
            throw new IllegalStateException("No more references");
        }
        return refs.get(refsOffset++);
    }

    public Cell preloadRef() {
        if (refsOffset >= refs.size()) {
            throw new IllegalStateException("No more references");
        }
        return refs.get(refsOffset);
    }

    public Cell loadMaybeRef() {
        return loadBit() ? loadRef() : null;
    }

    public Cell preloadMaybeRef() {
        return preloadBit() ? preloadRef() : null;
    }

    /* ==================== buffers ==================== */

    public byte[] loadBuffer(int bytes) {
        return reader.loadBuffer(bytes);
    }

    public byte[] preloadBuffer(int bytes) {
        return reader.preloadBuffer(bytes);
    }

    /* ==================== strings ==================== */

    public String loadStringTail() {
        return Strings.readString(this);
    }

    public String loadMaybeStringTail() {
        return loadBit() ? Strings.readString(this) : null;
    }

    public String loadStringRefTail() {
        return Strings.readString(loadRef().beginParse());
    }

    public String loadMaybeStringRefTail() {
        Cell ref = loadMaybeRef();
        return ref != null ? Strings.readString(ref.beginParse()) : null;
    }

    /* ==================== dictionaries ==================== */

    public <K, V> Dictionary<K, V> loadDict(
            Dictionary.DictionaryKey<K> key,
            Dictionary.DictionaryValue<V> value
    ) {
        return Dictionary.load(key, value, this);
    }

    public <K, V> Dictionary<K, V> loadDictDirect(
            Dictionary.DictionaryKey<K> key,
            Dictionary.DictionaryValue<V> value
    ) {
        return Dictionary.loadDirect(key, value, this);
    }

    /* ==================== finalize ==================== */

    public void endParse() {
        if (remainingBits() > 0 || remainingRefs() > 0) {
            throw new IllegalStateException("Slice is not empty");
        }
    }

    public Cell asCell() {
        return Builder.beginCell().storeSlice(this).endCell();
    }

    public Builder asBuilder() {
        return Builder.beginCell().storeSlice(this);
    }

    public Slice cloneSlice() {
        return cloneSlice(false);
    }

    public Slice cloneSlice(boolean fromStart) {
        if (fromStart) {
            BitReader r = reader.cloneReader();
            r.reset();
            return new Slice(r, refs);
        } else {
            Slice s = new Slice(reader, refs);
            s.refsOffset = this.refsOffset;
            return s;
        }
    }

    @Override
    public String toString() {
        return asCell().toString();
    }
}
