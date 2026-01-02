package dev.quark.ton.core.boc;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.address.ExternalAddress;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Ported from ton-core/src/boc/BitBuilder.ts
 */
public final class BitBuilder {

    private final byte[] buffer;
    private int length; // bits written

    public BitBuilder() {
        this(1023);
    }

    public BitBuilder(int sizeBits) {
        if (sizeBits < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
        this.buffer = new byte[(int) Math.ceil(sizeBits / 8.0)];
        this.length = 0;
    }

    /**
     * Current number of bits written
     */
    public int length() {
        return length;
    }

    /**
     * Write a single bit.
     * @param value true => 1, false => 0
     */
    public void writeBit(boolean value) {
        int n = this.length;
        if (n > this.buffer.length * 8) { // keep TS semantics: ">" (not ">=")
            throw new IllegalStateException("BitBuilder overflow");
        }

        if (value) {
            int byteIndex = n >> 3;
            int bitIndex = 7 - (n & 7);
            buffer[byteIndex] = (byte) (buffer[byteIndex] | (1 << bitIndex));
        }

        this.length++;
    }

    /**
     * TS compatibility helper: true or positive => 1; false/zero/negative => 0.
     */
    public void writeBit(int value) {
        writeBit(value > 0);
    }

    /**
     * Copy bits from BitString
     */
    public void writeBits(BitString src) {
        for (int i = 0; i < src.length(); i++) {
            writeBit(src.at(i));
        }
    }

    /**
     * Write bits from byte buffer (8-bit chunks).
     */
    public void writeBuffer(byte[] src) {
        if (src == null) {
            throw new IllegalArgumentException("src is null");
        }

        // Special case for aligned offsets
        if (this.length % 8 == 0) {
            if (this.length + src.length * 8 > this.buffer.length * 8) {
                throw new IllegalStateException("BitBuilder overflow");
            }
            System.arraycopy(src, 0, this.buffer, this.length / 8, src.length);
            this.length += src.length * 8;
        } else {
            for (byte b : src) {
                writeUint(b & 0xFF, 8);
            }
        }
    }

    // ---- writeUint ----

    public void writeUint(long value, int bits) {
        writeUint(BigInteger.valueOf(value), bits);
    }

    public void writeUint(BigInteger value, int bits) {

        // Special case for 8 bits (byte aligned)
        if (bits == 8 && this.length % 8 == 0) {
            int v;
            try {
                v = value.intValueExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("value is out of range for " + bits + " bits. Got " + value);
            }
            if (v < 0 || v > 255) {
                throw new IllegalArgumentException("value is out of range for " + bits + " bits. Got " + value);
            }
            this.buffer[this.length / 8] = (byte) v;
            this.length += 8;
            return;
        }

        // Special case for 16 bits (byte aligned)
        if (bits == 16 && this.length % 8 == 0) {
            int v;
            try {
                v = value.intValueExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("value is out of range for " + bits + " bits. Got " + value);
            }
            // TS check is v > 65536 (note: 65536, not 65535). Keep same semantics.
            if (v < 0 || v > 65536) {
                throw new IllegalArgumentException("value is out of range for " + bits + " bits. Got " + value);
            }
            int pos = this.length / 8;
            this.buffer[pos] = (byte) ((v >> 8) & 0xFF);
            this.buffer[pos + 1] = (byte) (v & 0xFF);
            this.length += 16;
            return;
        }

        // Generic case
        if (bits < 0) {
            throw new IllegalArgumentException("invalid bit length. Got " + bits);
        }

        // Corner case: zero bits
        if (bits == 0) {
            if (!BigInteger.ZERO.equals(value)) {
                throw new IllegalArgumentException("value is not zero for " + bits + " bits. Got " + value);
            }
            return;
        }

        if (value.signum() < 0) {
            throw new IllegalArgumentException("bitLength is too small for a value " + value + ". Got " + bits);
        }

        BigInteger limit = BigInteger.ONE.shiftLeft(bits); // 2^bits
        if (value.compareTo(limit) >= 0) {
            throw new IllegalArgumentException("bitLength is too small for a value " + value + ". Got " + bits);
        }

        // Write bits MSB -> LSB
        for (int i = 0; i < bits; i++) {
            int bitIndex = bits - i - 1;
            writeBit(value.testBit(bitIndex));
        }
    }

    // ---- writeInt ----

    public void writeInt(long value, int bits) {
        writeInt(BigInteger.valueOf(value), bits);
    }

    public void writeInt(BigInteger value, int bits) {
        if (bits < 0) {
            throw new IllegalArgumentException("invalid bit length. Got " + bits);
        }

        // Corner case: zero bits
        if (bits == 0) {
            if (!BigInteger.ZERO.equals(value)) {
                throw new IllegalArgumentException("value is not zero for " + bits + " bits. Got " + value);
            }
            return;
        }

        // Corner case: one bit
        if (bits == 1) {
            if (!(BigInteger.ZERO.equals(value) || BigInteger.valueOf(-1).equals(value))) {
                throw new IllegalArgumentException("value is not zero or -1 for " + bits + " bits. Got " + value);
            }
            writeBit(BigInteger.valueOf(-1).equals(value));
            return;
        }

        BigInteger vBits = BigInteger.ONE.shiftLeft(bits - 1); // 2^(bits-1)
        if (value.compareTo(vBits.negate()) < 0 || value.compareTo(vBits) >= 0) {
            throw new IllegalArgumentException("value is out of range for " + bits + " bits. Got " + value);
        }

        // Write sign
        BigInteger v = value;
        if (v.signum() < 0) {
            writeBit(true);
            v = vBits.add(v); // vBits + (negative v)
        } else {
            writeBit(false);
        }

        // Write magnitude
        writeUint(v, bits - 1);
    }

    // ---- var ints/uints ----

    public void writeVarUint(long value, int headerBits) {
        writeVarUint(BigInteger.valueOf(value), headerBits);
    }

    public void writeVarUint(BigInteger value, int headerBits) {
        if (headerBits < 0) {
            throw new IllegalArgumentException("invalid bit length. Got " + headerBits);
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("value is negative. Got " + value);
        }

        // Corner case: zero
        if (BigInteger.ZERO.equals(value)) {
            writeUint(0, headerBits);
            return;
        }

        int bitLen = value.bitLength();          // number of bits to represent
        int sizeBytes = (bitLen + 7) / 8;        // ceil(bitLen/8)
        int sizeBits = sizeBytes * 8;

        writeUint(sizeBytes, headerBits);
        writeUint(value, sizeBits);
    }

    public void writeVarInt(long value, int headerBits) {
        writeVarInt(BigInteger.valueOf(value), headerBits);
    }

    public void writeVarInt(BigInteger value, int headerBits) {
        if (headerBits < 0) {
            throw new IllegalArgumentException("invalid bit length. Got " + headerBits);
        }

        // Corner case: zero
        if (BigInteger.ZERO.equals(value)) {
            writeUint(0, headerBits);
            return;
        }

        BigInteger abs = value.signum() >= 0 ? value : value.negate();
        int bitLen = abs.bitLength();
        int sizeBytes = 1 + (bitLen + 7) / 8; // TS: 1 + ceil(bitLen/8)
        int sizeBits = sizeBytes * 8;

        writeUint(sizeBytes, headerBits);
        writeInt(value, sizeBits);
    }

    /**
     * Write coins in varuint format (header bits = 4)
     */
    public void writeCoins(long amount) {
        writeCoins(BigInteger.valueOf(amount));
    }

    public void writeCoins(BigInteger amount) {
        writeVarUint(amount, 4);
    }

    /**
     * Write Address (internal/external) or null.
     * TS signature: writeAddress(address: Maybe<Address | ExternalAddress>)
     */
    public void writeAddress(Object address) {

        // Empty address
        if (address == null) {
            writeUint(0, 2);
            return;
        }

        // Internal address
        if (address instanceof Address a) {
            writeUint(2, 2);          // internal address tag
            writeUint(0, 1);          // no anycast
            writeInt(a.workChain, 8);
            writeBuffer(a.hash);
            return;
        }

        // External address
        if (address instanceof ExternalAddress ea) {
            writeUint(1, 2);                  // external address tag
            writeUint(ea.getBits(), 9);
            writeUint(ea.getValue(), ea.getBits());
            return;
        }

        throw new IllegalArgumentException("Invalid address. Got " + address);
    }

    /**
     * Build BitString from the internal buffer.
     */
    public BitString build() {
        return new BitString(this.buffer, 0, this.length);
    }

    /**
     * Build into byte array (must be byte-aligned).
     */
    public byte[] buffer() {
        if (this.length % 8 != 0) {
            throw new IllegalStateException("BitBuilder buffer is not byte aligned");
        }
        return Arrays.copyOf(this.buffer, this.length / 8);
    }
}
