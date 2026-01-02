package dev.quark.ton.core.boc;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.address.ExternalAddress;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Class for reading bit strings.
 *
 * Ported from ton-core/src/boc/BitReader.ts
 */
public final class BitReader {

    private final BitString bits;
    private int offset; // bit offset
    private final Deque<Integer> checkpoints = new ArrayDeque<>();

    public BitReader(BitString bits) {
        this(bits, 0);
    }

    public BitReader(BitString bits, int offset) {
        this.bits = bits;
        this.offset = offset;
    }

    /**
     * Offset in source bit string
     */
    public int offset() {
        return offset;
    }

    /**
     * Number of bits remaining
     */
    public int remaining() {
        return bits.length() - offset;
    }

    /**
     * Skip bits
     */
    public void skip(int countBits) {
        if (countBits < 0 || this.offset + countBits > this.bits.length()) {
            throw new IllegalArgumentException("Index " + (this.offset + countBits) + " is out of bounds");
        }
        this.offset += countBits;
    }

    /**
     * Reset to the beginning or latest checkpoint
     */
    public void reset() {
        if (!checkpoints.isEmpty()) {
            this.offset = checkpoints.removeLast();
        } else {
            this.offset = 0;
        }
    }

    /**
     * Save checkpoint
     */
    public void save() {
        checkpoints.addLast(this.offset);
    }

    /**
     * Load a single bit
     */
    public boolean loadBit() {
        boolean r = this.bits.at(this.offset);
        this.offset++;
        return r;
    }

    /**
     * Preload bit
     */
    public boolean preloadBit() {
        return this.bits.at(this.offset);
    }

    /**
     * Load bit string
     */
    public BitString loadBits(int countBits) {
        BitString r = this.bits.substring(this.offset, countBits);
        this.offset += countBits;
        return r;
    }

    /**
     * Preload bit string
     */
    public BitString preloadBits(int countBits) {
        return this.bits.substring(this.offset, countBits);
    }

    /**
     * Load buffer (bytes)
     */
    public byte[] loadBuffer(int bytes) {
        byte[] buf = preloadBuffer(bytes);
        this.offset += bytes * 8;
        return buf;
    }

    /**
     * Preload buffer (bytes)
     */
    public byte[] preloadBuffer(int bytes) {
        return preloadBufferAt(bytes, this.offset);
    }

    /**
     * Load uint as long (TS returns number, but in tests they keep <= 48 bits)
     */
    public long loadUint(int countBits) {
        return loadUintBig(countBits).longValueExact(); // loadUintBig уже двигает offset
    }

    public BigInteger loadUintBig(int countBits) {
        BigInteger loaded = preloadUintBig(countBits);
        this.offset += countBits;
        return loaded;
    }

    public long preloadUint(int countBits) {
        return preloadUintBig(countBits).longValueExact();
    }

    public BigInteger preloadUintBig(int countBits) {
        return preloadUintAt(countBits, this.offset);
    }

    /**
     * Load int as long (fits test ranges)
     */
    public long loadInt(int countBits) {
        BigInteger res = preloadIntAt(countBits, this.offset);
        this.offset += countBits;
        return res.longValueExact();
    }

    public BigInteger loadIntBig(int countBits) {
        BigInteger res = preloadIntAt(countBits, this.offset);
        this.offset += countBits;
        return res;
    }

    public long preloadInt(int countBits) {
        return preloadIntAt(countBits, this.offset).longValueExact();
    }

    public BigInteger preloadIntBig(int countBits) {
        return preloadIntAt(countBits, this.offset);
    }

    // ---- VarUInt / VarInt ----

    public long loadVarUint(int headerBits) {
        int size = (int) loadUint(headerBits);
        return loadUintBig(size * 8).longValueExact();
    }

    public BigInteger loadVarUintBig(int headerBits) {
        int size = (int) loadUint(headerBits);
        return loadUintBig(size * 8);
    }

    public long preloadVarUint(int headerBits) {
        int size = preloadUintAt(headerBits, this.offset).intValueExact();
        return preloadUintAt(size * 8, this.offset + headerBits).longValueExact();
    }

    public BigInteger preloadVarUintBig(int headerBits) {
        int size = preloadUintAt(headerBits, this.offset).intValueExact();
        return preloadUintAt(size * 8, this.offset + headerBits);
    }

    public long loadVarInt(int headerBits) {
        int size = (int) loadUint(headerBits);
        return loadIntBig(size * 8).longValueExact();
    }

    public BigInteger loadVarIntBig(int headerBits) {
        int size = (int) loadUint(headerBits);
        return loadIntBig(size * 8);
    }

    public long preloadVarInt(int headerBits) {
        int size = preloadUintAt(headerBits, this.offset).intValueExact();
        return preloadIntAt(size * 8, this.offset + headerBits).longValueExact();
    }

    public BigInteger preloadVarIntBig(int headerBits) {
        int size = preloadUintAt(headerBits, this.offset).intValueExact();
        return preloadIntAt(size * 8, this.offset + headerBits);
    }

    // ---- Coins ----

    public BigInteger loadCoins() {
        return loadVarUintBig(4);
    }

    public BigInteger preloadCoins() {
        return preloadVarUintBig(4);
    }

    // ---- Addresses ----

    public Address loadAddress() {
        int type = preloadUintAt(2, this.offset).intValueExact();
        if (type == 2) {
            return loadInternalAddress();
        }
        throw new IllegalArgumentException("Invalid address: " + type);
    }

    public Address loadMaybeAddress() {
        int type = preloadUintAt(2, this.offset).intValueExact();
        if (type == 0) {
            this.offset += 2;
            return null;
        } else if (type == 2) {
            return loadInternalAddress();
        } else {
            throw new IllegalArgumentException("Invalid address");
        }
    }

    public ExternalAddress loadExternalAddress() {
        int type = preloadUintAt(2, this.offset).intValueExact();
        if (type == 1) {
            return loadExternalAddressImpl();
        }
        throw new IllegalArgumentException("Invalid address");
    }

    public ExternalAddress loadMaybeExternalAddress() {
        int type = preloadUintAt(2, this.offset).intValueExact();
        if (type == 0) {
            this.offset += 2;
            return null;
        } else if (type == 1) {
            return loadExternalAddressImpl();
        } else {
            throw new IllegalArgumentException("Invalid address");
        }
    }

    /**
     * Read address of any type: Address | ExternalAddress | null
     * For now, return Object to keep same semantics.
     */
    public Object loadAddressAny() {
        int type = preloadUintAt(2, this.offset).intValueExact();
        if (type == 0) {
            this.offset += 2;
            return null;
        } else if (type == 2) {
            return loadInternalAddress();
        } else if (type == 1) {
            return loadExternalAddressImpl();
        } else if (type == 3) {
            throw new IllegalStateException("Unsupported");
        } else {
            throw new IllegalStateException("Unreachable");
        }
    }

    /**
     * Load bit string that was padded to make it byte-aligned. Used in BOC serialization.
     * @param bitsCount number of bits to read (must be multiple of 8)
     */
    public BitString loadPaddedBits(int bitsCount) {
        if (bitsCount % 8 != 0) {
            throw new IllegalArgumentException("Invalid number of bits");
        }

        int length = bitsCount;
        while (true) {
            if (this.bits.at(this.offset + length - 1)) {
                length--;
                break;
            } else {
                length--;
            }
        }

        BitString r = this.bits.substring(this.offset, length);
        this.offset += bitsCount;
        return r;
    }

    public BitReader cloneReader() {
        return new BitReader(this.bits, this.offset);
    }

    // ---- internal preload helpers (ported 1:1) ----

    private BigInteger preloadIntAt(int countBits, int atOffset) {
        if (countBits == 0) {
            return BigInteger.ZERO;
        }

        boolean sign = this.bits.at(atOffset);
        BigInteger res = BigInteger.ZERO;

        for (int i = 0; i < countBits - 1; i++) {
            if (this.bits.at(atOffset + 1 + i)) {
                int shift = (countBits - i - 1 - 1);
                res = res.add(BigInteger.ONE.shiftLeft(shift));
            }
        }

        if (sign) {
            res = res.subtract(BigInteger.ONE.shiftLeft(countBits - 1));
        }
        return res;
    }

    private BigInteger preloadUintAt(int countBits, int atOffset) {
        if (countBits == 0) {
            return BigInteger.ZERO;
        }

        BigInteger res = BigInteger.ZERO;
        for (int i = 0; i < countBits; i++) {
            if (this.bits.at(atOffset + i)) {
                int shift = (countBits - i - 1);
                res = res.add(BigInteger.ONE.shiftLeft(shift));
            }
        }
        return res;
    }

    private byte[] preloadBufferAt(int bytes, int atOffset) {

        // Try fast path
        byte[] fast = this.bits.subbuffer(atOffset, bytes * 8);
        if (fast != null) {
            return fast;
        }

        // Slow path
        byte[] buf = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            buf[i] = (byte) preloadUintAt(8, atOffset + i * 8).intValueExact();
        }
        return buf;
    }

    private Address loadInternalAddress() {

        int type = preloadUintAt(2, this.offset).intValueExact();
        if (type != 2) {
            throw new IllegalArgumentException("Invalid address");
        }

        // No Anycast supported
        if (!preloadUintAt(1, this.offset + 2).equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("Invalid address");
        }

        int wc = preloadIntAt(8, this.offset + 3).intValueExact();
        byte[] hash = preloadBufferAt(32, this.offset + 11);

        // Update offset: 2 + 1 + 8 + 256 = 267 bits
        this.offset += 267;

        return new Address(wc, hash);
    }

    private ExternalAddress loadExternalAddressImpl() {

        int type = preloadUintAt(2, this.offset).intValueExact();
        if (type != 1) {
            throw new IllegalArgumentException("Invalid address");
        }

        int bitsCount = preloadUintAt(9, this.offset + 2).intValueExact();
        BigInteger value = preloadUintAt(bitsCount, this.offset + 11);

        this.offset += 11 + bitsCount;

        return new ExternalAddress(value, bitsCount);
    }
}
