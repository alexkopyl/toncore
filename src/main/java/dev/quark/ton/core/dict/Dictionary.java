package dev.quark.ton.core.dict;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.utils.InternalKeySerializer;

import java.math.BigInteger;
import java.util.*;

/**
 * 1:1 port of ton-core Dictionary.ts
 *
 * TS:
 *  - DictionaryKeyTypes = Address | number | bigint | Buffer | BitString
 *  - internal Map<string,V> key = serializeInternalKey(key)
 */
public final class Dictionary<K, V> implements Iterable<Map.Entry<K, V>> {

    // ===== TS types =====

    public interface DictionaryKey<K> {
        int bits();
        BigInteger serialize(K src);
        K parse(BigInteger src);
    }

    public interface DictionaryValue<V> {
        void serialize(V src, Builder builder);
        V parse(Slice slice);
    }


    // ===== TS: static Keys =====

    public static final class Keys {
        private Keys() {}

        public static DictionaryKey<Address> Address() {
            return createAddressKey();
        }

        public static DictionaryKey<BigInteger> BigInt(int bits) {
            return createBigIntKey(bits);
        }

        public static DictionaryKey<Long> Int(int bits) {
            return createIntKey(bits);
        }

        public static DictionaryKey<BigInteger> BigUint(int bits) {
            return createBigUintKey(bits);
        }

        public static DictionaryKey<Long> Uint(int bits) {
            return createUintKey(bits);
        }

        /** TS Buffer -> Java byte[] */
        public static DictionaryKey<byte[]> Buffer(int bytes) {
            return createBufferKey(bytes);
        }

        public static DictionaryKey<BitString> BitString(int bits) {
            return createBitStringKey(bits);
        }
    }

    // ===== TS: static Values =====

    public static final class Values {
        private Values() {}

        public static DictionaryValue<BigInteger> BigInt(int bits) {
            return createBigIntValue(bits);
        }

        public static DictionaryValue<Long> Int(int bits) {
            return createIntValue(bits);
        }

        public static DictionaryValue<BigInteger> BigVarInt(int headerBits) {
            return createBigVarIntValue(headerBits);
        }

        public static DictionaryValue<BigInteger> BigUint(int bits) {
            return createBigUintValue(bits);
        }

        public static DictionaryValue<Long> Uint(int bits) {
            return createUintValue(bits);
        }

        public static DictionaryValue<BigInteger> BigVarUint(int headerBits) {
            return createBigVarUintValue(headerBits);
        }

        public static DictionaryValue<Boolean> Bool() {
            return createBooleanValue();
        }

        public static DictionaryValue<Address> Address() {
            return createAddressValue();
        }

        public static DictionaryValue<Cell> Cell() {
            return createCellValue();
        }

        /** TS Buffer -> Java byte[] */
        public static DictionaryValue<byte[]> Buffer(int bytes) {
            return createBufferValue(bytes);
        }

        public static DictionaryValue<BitString> BitString(int bits) {
            return createBitStringValue(bits);
        }

        public static <K, V> DictionaryValue<Dictionary<K, V>> Dictionary(DictionaryKey<K> key, DictionaryValue<V> value) {
            return createDictionaryValue(key, value);
        }
    }

    // ===== TS: empty/load/loadDirect =====

    public static <K, V> Dictionary<K, V> empty(DictionaryKey<K> key, DictionaryValue<V> value) {
        return new Dictionary<>(new LinkedHashMap<>(), key, value);
    }

    public static <K, V> Dictionary<K, V> empty() {
        return new Dictionary<>(new LinkedHashMap<>(), null, null);
    }

    public static <K, V> Dictionary<K, V> load(DictionaryKey<K> key, DictionaryValue<V> value, Object sc /* Slice|Cell */) {
        final Slice slice;
        if (sc instanceof Cell c) {
            if (c.isExotic()) {
                return Dictionary.empty(key, value);
            }
            slice = c.beginParse();
        } else if (sc instanceof Slice s) {
            slice = s;
        } else {
            throw new IllegalArgumentException("sc must be Slice or Cell");
        }

        Cell cell = slice.loadMaybeRef();
        if (cell != null && !cell.isExotic()) {
            return Dictionary.loadDirect(key, value, cell.beginParse());
        } else {
            return Dictionary.empty(key, value);
        }
    }

    public static <K, V> Dictionary<K, V> loadDirect(DictionaryKey<K> key, DictionaryValue<V> value, Object sc /* Slice|Cell|null */) {
        if (sc == null) {
            return Dictionary.empty(key, value);
        }

        final Slice slice;
        if (sc instanceof Cell c) {
            slice = c.beginParse();
        } else if (sc instanceof Slice s) {
            slice = s;
        } else {
            throw new IllegalArgumentException("sc must be Slice or Cell");
        }

        // TS: let values = parseDict(slice, key.bits, value.parse);
        Map<BigInteger, V> values = ParseDict.parseDict(slice, key.bits(), value::parse);

        // TS: prepare.set(serializeInternalKey(key.parse(k)), v);
        Map<String, V> prepare = new LinkedHashMap<>();
        for (Map.Entry<BigInteger, V> e : values.entrySet()) {
            K kk = key.parse(e.getKey());
            prepare.put(InternalKeySerializer.serializeInternalKey(kk), e.getValue());
        }

        return new Dictionary<>(prepare, key, value);
    }

    // ===== instance fields =====

    private final DictionaryKey<K> _key;          // nullable like TS
    private final DictionaryValue<V> _value;      // nullable like TS
    private final Map<String, V> _map;

    private Dictionary(Map<String, V> values, DictionaryKey<K> key, DictionaryValue<V> value) {
        this._key = key;
        this._value = value;
        this._map = values;
    }

    public int size() {
        return _map.size();
    }

    public V get(K key) {
        return _map.get(InternalKeySerializer.serializeInternalKey(key));
    }

    public boolean has(K key) {
        return _map.containsKey(InternalKeySerializer.serializeInternalKey(key));
    }

    public Dictionary<K, V> set(K key, V value) {
        _map.put(InternalKeySerializer.serializeInternalKey(key), value);
        return this;
    }

    public boolean delete(K key) {
        String k = InternalKeySerializer.serializeInternalKey(key);
        boolean existed = _map.containsKey(k);
        _map.remove(k);
        return existed;
    }

    public void clear() {
        _map.clear();
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        final Iterator<Map.Entry<String, V>> it = _map.entrySet().iterator();
        return new Iterator<>() {
            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public Map.Entry<K, V> next() {
                Map.Entry<String, V> e = it.next();
                @SuppressWarnings("unchecked")
                K key = (K) InternalKeySerializer.deserializeInternalKey(e.getKey());
                return new AbstractMap.SimpleImmutableEntry<>(key, e.getValue());
            }
        };
    }

    public List<K> keys() {
        List<K> res = new ArrayList<>(_map.size());
        for (String k : _map.keySet()) {
            @SuppressWarnings("unchecked")
            K key = (K) InternalKeySerializer.deserializeInternalKey(k);
            res.add(key);
        }
        return res;
    }

    public List<V> values() {
        return new ArrayList<>(_map.values());
    }

    public void store(Builder builder, DictionaryKey<K> key, DictionaryValue<V> value) {
        if (_map.size() == 0) {
            builder.storeBit(false);
            return;
        }

        // Resolve serializer (TS logic)
        DictionaryKey<K> resolvedKey = _key;
        if (key != null) resolvedKey = key;

        DictionaryValue<V> resolvedValue = _value;
        if (value != null) resolvedValue = value;

        if (resolvedKey == null) throw new IllegalStateException("Key serializer is not defined");
        if (resolvedValue == null) throw new IllegalStateException("Value serializer is not defined");

        // Prepare map: prepared.set(resolvedKey.serialize(deserializeInternalKey(k)), v);
        Map<BigInteger, V> prepared = new LinkedHashMap<>();
        for (Map.Entry<String, V> e : _map.entrySet()) {
            Object dk = InternalKeySerializer.deserializeInternalKey(e.getKey());
            @SuppressWarnings("unchecked")
            K typed = (K) dk;
            prepared.put(resolvedKey.serialize(typed), e.getValue());
        }

        // Store
        builder.storeBit(true);
        Builder dd = Builder.beginCell();
        SerializeDict.serializeDict(prepared, resolvedKey.bits(), resolvedValue::serialize, dd);
        builder.storeRef(dd.endCell());
    }

    public void store(Builder builder) {
        store(builder, null, null);
    }

    public void storeDirect(Builder builder, DictionaryKey<K> key, DictionaryValue<V> value) {
        if (_map.size() == 0) {
            throw new IllegalStateException("Cannot store empty dictionary directly");
        }

        // Resolve serializer
        DictionaryKey<K> resolvedKey = _key;
        if (key != null) resolvedKey = key;

        DictionaryValue<V> resolvedValue = _value;
        if (value != null) resolvedValue = value;

        if (resolvedKey == null) throw new IllegalStateException("Key serializer is not defined");
        if (resolvedValue == null) throw new IllegalStateException("Value serializer is not defined");

        // Prepare map
        Map<BigInteger, V> prepared = new LinkedHashMap<>();
        for (Map.Entry<String, V> e : _map.entrySet()) {
            Object dk = InternalKeySerializer.deserializeInternalKey(e.getKey());
            @SuppressWarnings("unchecked")
            K typed = (K) dk;
            prepared.put(resolvedKey.serialize(typed), e.getValue());
        }

        // Store direct
        SerializeDict.serializeDict(prepared, resolvedKey.bits(), resolvedValue::serialize, builder);
    }

    public Cell generateMerkleProof(K key) {
        if (_key == null) throw new IllegalStateException("Key serializer is not defined");
        if (_value == null) throw new IllegalStateException("Value serializer is not defined");
        return GenerateMerkleProof.generateMerkleProof(this, key, _key, _value);
    }

    public Cell generateMerkleUpdate(K key, V newValue) {
        if (_key == null) throw new IllegalStateException("Key serializer is not defined");
        if (_value == null) throw new IllegalStateException("Value serializer is not defined");
        return GenerateMerkleUpdate.generateMerkleUpdate(this, key, _key, _value, newValue);
    }

    // =========================================================================
    // Keys and Values (private factory functions) - 1:1 with TS bottom section
    // =========================================================================

    private static DictionaryKey<Address> createAddressKey() {
        return new DictionaryKey<>() {
            @Override public int bits() { return 267; }

            @Override
            public BigInteger serialize(Address src) {
                if (!Address.isAddress(src)) {
                    throw new IllegalArgumentException("Key is not an address");
                }
                return Builder.beginCell()
                        .storeAddress(src)
                        .endCell()
                        .beginParse()
                        .preloadUintBig(267);
            }

            @Override
            public Address parse(BigInteger src) {
                return Builder.beginCell()
                        .storeUint(src, 267)
                        .endCell()
                        .beginParse()
                        .loadAddress();
            }
        };
    }

    private static DictionaryKey<BigInteger> createBigIntKey(int bits) {
        return new DictionaryKey<>() {
            @Override public int bits() { return bits; }

            @Override
            public BigInteger serialize(BigInteger src) {
                if (src == null) throw new IllegalArgumentException("Key is not a bigint");
                return Builder.beginCell()
                        .storeInt(src, bits)
                        .endCell()
                        .beginParse()
                        .loadUintBig(bits);
            }

            @Override
            public BigInteger parse(BigInteger src) {
                return Builder.beginCell()
                        .storeUint(src, bits)
                        .endCell()
                        .beginParse()
                        .loadIntBig(bits);
            }
        };
    }

    private static DictionaryKey<Long > createIntKey(int bits) {
        return new DictionaryKey<>() {
            @Override public int bits() { return bits; }

            @Override
            public BigInteger serialize(Long src) {
                if (src == null) throw new IllegalArgumentException("Key is not a number");
                return Builder.beginCell()
                        .storeInt(src, bits)
                        .endCell()
                        .beginParse()
                        .loadUintBig(bits);
            }

            @Override
            public Long parse(BigInteger src) {
                return Builder.beginCell()
                        .storeUint(src, bits)
                        .endCell()
                        .beginParse()
                        .loadInt(bits);
            }
        };
    }

    private static DictionaryKey<BigInteger> createBigUintKey(int bits) {
        return new DictionaryKey<>() {
            @Override public int bits() { return bits; }

            @Override
            public BigInteger serialize(BigInteger src) {
                if (src == null) throw new IllegalArgumentException("Key is not a bigint");
                if (src.signum() < 0) throw new IllegalArgumentException("Key is negative: " + src);
                return Builder.beginCell()
                        .storeUint(src, bits)
                        .endCell()
                        .beginParse()
                        .loadUintBig(bits);
            }

            @Override
            public BigInteger parse(BigInteger src) {
                return Builder.beginCell()
                        .storeUint(src, bits)
                        .endCell()
                        .beginParse()
                        .loadUintBig(bits);
            }
        };
    }

    private static DictionaryKey<Long> createUintKey(int bits) {
        return new DictionaryKey<>() {
            @Override public int bits() { return bits; }

            @Override
            public BigInteger serialize(Long src) {
                if (src == null) throw new IllegalArgumentException("Key is not a number");
                if (src < 0) throw new IllegalArgumentException("Key is negative: " + src);
                return Builder.beginCell()
                        .storeUint(src, bits)
                        .endCell()
                        .beginParse()
                        .loadUintBig(bits);
            }

            @Override
            public Long parse(BigInteger src) {
                // TS: Number(loadUint(bits))
                return Builder.beginCell()
                        .storeUint(src, bits)
                        .endCell()
                        .beginParse()
                        .loadUint(bits);
            }
        };
    }

    private static DictionaryKey<byte[]> createBufferKey(int bytes) {
        return new DictionaryKey<>() {
            @Override public int bits() { return bytes * 8; }

            @Override
            public BigInteger serialize(byte[] src) {
                if (src == null) throw new IllegalArgumentException("Key is not a buffer");
                return Builder.beginCell()
                        .storeBuffer(src)
                        .endCell()
                        .beginParse()
                        .loadUintBig(bytes * 8);
            }

            @Override
            public byte[] parse(BigInteger src) {
                return Builder.beginCell()
                        .storeUint(src, bytes * 8)
                        .endCell()
                        .beginParse()
                        .loadBuffer(bytes);
            }
        };
    }

    private static DictionaryKey<BitString> createBitStringKey(int bits) {
        return new DictionaryKey<>() {
            @Override public int bits() { return bits; }

            @Override
            public BigInteger serialize(BitString src) {
                if (src == null) {
                    throw new IllegalArgumentException("Key is not a BitString");
                }
                return Builder.beginCell()
                        .storeBits(src)
                        .endCell()
                        .beginParse()
                        .loadUintBig(bits);
            }

            @Override
            public BitString parse(BigInteger src) {
                return Builder.beginCell()
                        .storeUint(src, bits)
                        .endCell()
                        .beginParse()
                        .loadBits(bits);
            }
        };
    }

    private static DictionaryValue<Long> createIntValue(int bits) {
        return new DictionaryValue<>() {
            @Override public void serialize(Long src, Builder builder) { builder.storeInt(src, bits); }
            @Override
            public Long parse(Slice src) {
                long v = src.loadInt(bits);
                src.endParse();
                return v;
            }

        };
    }

    private static DictionaryValue<BigInteger> createBigIntValue(int bits) {
        return new DictionaryValue<>() {
            @Override public void serialize(BigInteger src, Builder builder) { builder.storeInt(src, bits); }
            @Override
            public BigInteger parse(Slice src) {
                BigInteger v = src.loadIntBig(bits);
                src.endParse();
                return v;
            }
        };
    }

    private static DictionaryValue<BigInteger> createBigVarIntValue(int headerBits) {
        return new DictionaryValue<>() {
            @Override public void serialize(BigInteger src, Builder builder) { builder.storeVarInt(src, headerBits); }
            @Override
            public BigInteger parse(Slice src) {
                BigInteger v = src.loadVarIntBig(headerBits);
                src.endParse();
                return v;
            }
        };
    }

    private static DictionaryValue<BigInteger> createBigVarUintValue(int headerBits) {
        return new DictionaryValue<>() {
            @Override public void serialize(BigInteger src, Builder builder) { builder.storeVarUint(src, headerBits); }
            @Override
            public BigInteger parse(Slice src) {
                BigInteger v = src.loadVarUintBig(headerBits);
                src.endParse();
                return v;
            }
        };
    }

    private static DictionaryValue<Long> createUintValue(int bits) {
        return new DictionaryValue<>() {
            @Override public void serialize(Long src, Builder builder) { builder.storeUint(src, bits); }
            @Override
            public Long parse(Slice src) {
                long v = src.loadUint(bits);
                src.endParse();
                return v;
            }
        };
    }

    private static DictionaryValue<BigInteger> createBigUintValue(int bits) {
        return new DictionaryValue<>() {
            @Override public void serialize(BigInteger src, Builder builder) { builder.storeUint(src, bits); }
            @Override
            public BigInteger parse(Slice src) {
                BigInteger v = src.loadUintBig(bits);
                src.endParse();
                return v;
            }
        };
    }

    private static DictionaryValue<Boolean> createBooleanValue() {
        return new DictionaryValue<>() {
            @Override public void serialize(Boolean src, Builder builder) { builder.storeBit(Boolean.TRUE.equals(src)); }
            @Override
            public Boolean parse(Slice src) {
                boolean v = src.loadBit();
                src.endParse();
                return v;
            }
        };
    }

    private static DictionaryValue<Address> createAddressValue() {
        return new DictionaryValue<>() {
            @Override public void serialize(Address src, Builder builder) { builder.storeAddress(src); }
            @Override
            public Address parse(Slice src) {
                Address v = src.loadAddress();
                src.endParse();
                return v;
            }
        };
    }

    private static DictionaryValue<Cell> createCellValue() {
        return new DictionaryValue<>() {
            @Override public void serialize(Cell src, Builder builder) { builder.storeRef(src); }
            @Override
            public Cell parse(Slice src) {
                Cell v = src.loadRef();
                src.endParse();
                return v;
            }
        };
    }

    private static <K, V> DictionaryValue<Dictionary<K, V>> createDictionaryValue(DictionaryKey<K> key, DictionaryValue<V> value) {
        return new DictionaryValue<>() {
            @Override
            public void serialize(Dictionary<K, V> src, Builder builder) {
                src.store(builder, key, value);
            }

            @Override
            public Dictionary<K, V> parse(Slice src) {
                src.endParse();
                return Dictionary.load(key, value, src);
            }
        };
    }


    private static DictionaryValue<byte[]> createBufferValue(int size) {
        return new DictionaryValue<>() {
            @Override
            public void serialize(byte[] src, Builder builder) {
                if (src == null) throw new IllegalArgumentException("Invalid buffer size");
                if (src.length != size) throw new IllegalArgumentException("Invalid buffer size");
                builder.storeBuffer(src);
            }

            @Override
            public byte[] parse(Slice src) {
                byte[] v = src.loadBuffer(size);
                src.endParse();
                return v;
            }
        };
    }

    private static DictionaryValue<BitString> createBitStringValue(int bits) {
        return new DictionaryValue<>() {
            @Override
            public void serialize(BitString src, Builder builder) {
                if (src.length() != bits) throw new IllegalArgumentException("Invalid BitString size");
                builder.storeBits(src);
            }

            @Override
            public BitString parse(Slice src) {
                BitString v = src.loadBits(bits);
                src.endParse();
                return v;
            }
        };
    }
}
