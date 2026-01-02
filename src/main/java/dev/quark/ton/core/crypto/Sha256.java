package dev.quark.ton.core.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha256 {

    private Sha256() {}

    /** Equivalent to sha256_sync(Buffer | string) in ton-crypto. */
    public static byte[] sha256Sync(byte[] source) {
        if (source == null) {
            throw new IllegalArgumentException("source is null");
        }
        return sha256(source);
    }

    /** Equivalent to sha256_sync(Buffer | string) in ton-crypto (string treated as UTF-8). */
    public static byte[] sha256Sync(String source) {
        if (source == null) {
            throw new IllegalArgumentException("source is null");
        }
        return sha256(source.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen on a standard JDK
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
