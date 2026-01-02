package dev.quark.ton.core.crypto;

import dev.quark.ton.core.boc.Cell;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * 1:1 port of ton-core/src/crypto/safeSign.ts
 *
 * TS:
 *  safeSign(cell, secretKey, seed?) -> sign(sha256(0xffff || seed || cell.hash()), secretKey)
 *  safeSignVerify(cell, signature, publicKey, seed?) -> signVerify(...)
 */
public final class SafeSign {

    private static final int MIN_SEED_LENGTH = 8;
    private static final int MAX_SEED_LENGTH = 64;

    private static final String DEFAULT_SEED = "ton-safe-sign-magic";

    private SafeSign() {
    }

    public static byte[] safeSign(Cell cell, byte[] secretKey) {
        return safeSign(cell, secretKey, DEFAULT_SEED);
    }

    public static byte[] safeSign(Cell cell, byte[] secretKey, String seed) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(secretKey, "secretKey");
        Objects.requireNonNull(seed, "seed");

        byte[] hash = createSafeSignHash(cell, seed);

        byte[] skSeed = normalizeSecretKeyToSeed32(secretKey);

        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, new Ed25519PrivateKeyParameters(skSeed, 0));
        signer.update(hash, 0, hash.length);
        return signer.generateSignature(); // 64 bytes
    }

    public static boolean safeSignVerify(Cell cell, byte[] signature, byte[] publicKey) {
        return safeSignVerify(cell, signature, publicKey, DEFAULT_SEED);
    }

    public static boolean safeSignVerify(Cell cell, byte[] signature, byte[] publicKey, String seed) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(signature, "signature");
        Objects.requireNonNull(publicKey, "publicKey");
        Objects.requireNonNull(seed, "seed");

        if (signature.length != 64) {
            return false;
        }
        if (publicKey.length != 32) {
            return false;
        }

        byte[] hash = createSafeSignHash(cell, seed);

        Ed25519Signer verifier = new Ed25519Signer();
        verifier.init(false, new Ed25519PublicKeyParameters(publicKey, 0));
        verifier.update(hash, 0, hash.length);
        return verifier.verifySignature(signature);
    }

    // ---------------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------------

    private static byte[] createSafeSignHash(Cell cell, String seed) {
        byte[] seedData = seed.getBytes(StandardCharsets.UTF_8);

        if (seedData.length > MAX_SEED_LENGTH) {
            throw new IllegalArgumentException("Seed can't be longer than 64 bytes");
        }
        if (seedData.length < MIN_SEED_LENGTH) {
            throw new IllegalArgumentException("Seed must be at least 8 bytes");
        }

        byte[] cellHash = cell.hash(); // ожидаем 32 bytes как в ton-core
        byte[] data = new byte[2 + seedData.length + cellHash.length];

        data[0] = (byte) 0xFF;
        data[1] = (byte) 0xFF;
        System.arraycopy(seedData, 0, data, 2, seedData.length);
        System.arraycopy(cellHash, 0, data, 2 + seedData.length, cellHash.length);

        return sha256(data);
    }

    /**
     * ton-crypto sign() часто использует secretKey как 64 bytes (private+public),
     * либо 32 bytes seed. Делаем поддержку обоих вариантов.
     */
    private static byte[] normalizeSecretKeyToSeed32(byte[] secretKey) {
        if (secretKey.length == 32) {
            return secretKey;
        }
        if (secretKey.length == 64) {
            byte[] seed = new byte[32];
            System.arraycopy(secretKey, 0, seed, 0, 32);
            return seed;
        }
        throw new IllegalArgumentException("Invalid secretKey length: " + secretKey.length + " (expected 32 or 64)");
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 обязателен в любой нормальной JVM
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
