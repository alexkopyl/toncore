package dev.quark.ton.core.address;

import dev.quark.ton.core.utils.Base32;
import dev.quark.ton.core.utils.Crc16;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public final class ADNLAddress {

    private final byte[] address; // 32 bytes

    public ADNLAddress(byte[] address) {
        if (address == null || address.length != 32) {
            throw new IllegalArgumentException("Invalid address");
        }
        this.address = Arrays.copyOf(address, address.length);
    }

    public static ADNLAddress parseFriendly(String src) {
        if (src == null || src.length() != 55) {
            throw new IllegalArgumentException("Invalid address");
        }

        // Decoding: src = 'f' + src
        byte[] decoded;
        try {
            decoded = Base32.base32Decode("f" + src);
        } catch (RuntimeException e) {
            // на случай если декодер кидает исключения
            throw new IllegalArgumentException("Invalid address");
        }

        // Must be 1 (tag) + 32 (address) + 2 (crc16) = 35 bytes
        if (decoded.length != 35) {
            throw new IllegalArgumentException("Invalid address");
        }

        if ((decoded[0] & 0xFF) != 0x2D) {
            throw new IllegalArgumentException("Invalid address");
        }

        byte[] toHash = Arrays.copyOfRange(decoded, 0, 33);   // [0..32]
        byte[] gotHash = Arrays.copyOfRange(decoded, 33, 35); // last 2 bytes
        byte[] hash = Crc16.crc16(toHash);                    // must return 2 bytes

        if (hash.length != 2 || !Arrays.equals(hash, gotHash)) {
            throw new IllegalArgumentException("Invalid address");
        }

        return new ADNLAddress(Arrays.copyOfRange(decoded, 1, 33));
    }

    public static ADNLAddress parseRaw(String src) {
        if (src == null) {
            throw new IllegalArgumentException("Invalid address");
        }
        byte[] data;
        try {
            data = Base64.getDecoder().decode(src);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid address");
        }
        return new ADNLAddress(data);
    }

    public byte[] getAddress() {
        return Arrays.copyOf(address, address.length);
    }

    // TS-style helper
    public boolean equals(ADNLAddress b) {
        return b != null && Arrays.equals(this.address, b.address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ADNLAddress other)) return false;
        return Arrays.equals(address, other.address);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    public String toRaw() {
        return toHexUpper(address);
    }

    @Override
    public String toString() {
        byte[] data = new byte[1 + 32];
        data[0] = 0x2D;
        System.arraycopy(address, 0, data, 1, 32);

        byte[] hash = Crc16.crc16(data);
        if (hash.length != 2) {
            throw new IllegalStateException("Invalid crc16 implementation");
        }

        byte[] withHash = new byte[data.length + 2];
        System.arraycopy(data, 0, withHash, 0, data.length);
        System.arraycopy(hash, 0, withHash, data.length, 2);

        // base32Encode(...).slice(1)
        String enc = Base32.base32Encode(withHash);
        if (enc.length() < 1) {
            throw new IllegalStateException("Invalid base32 implementation");
        }
        return enc.substring(1);
    }

    private static String toHexUpper(byte[] bytes) {
        char[] hex = "0123456789ABCDEF".toCharArray();
        char[] out = new char[bytes.length * 2];
        int j = 0;
        for (byte b : bytes) {
            int v = b & 0xFF;
            out[j++] = hex[v >>> 4];
            out[j++] = hex[v & 0x0F];
        }
        return new String(out);
    }
}
