import dev.quark.ton.core.crypto.Sha256;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class Sha256Test {

    private record Vec(String value, String outputHex) {}

    // Test vectors from https://www.di-mgt.com.au/sha_testvectors.html (как в sha256.spec.ts)
    private static final Vec[] VECTORS = new Vec[] {
            new Vec("abc", "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
            new Vec("", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
            new Vec("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
                    "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1")
    };

    @Test
    void shouldProcessTestVectors_StringAndBytes() {
        for (Vec v : VECTORS) {
            byte[] expected = hexToBytes(v.outputHex());

            byte[] resStr = Sha256.sha256Sync(v.value());
            assertArrayEquals(expected, resStr, "String input mismatch for: " + v.value());

            byte[] resBytes = Sha256.sha256Sync(v.value().getBytes(StandardCharsets.UTF_8));
            assertArrayEquals(expected, resBytes, "Bytes input mismatch for: " + v.value());

            // sanity: length always 32 bytes
            assertEquals(32, resStr.length);
            assertEquals(32, resBytes.length);
        }
    }

    @Test
    void shouldThrowOnNullInput() {
        assertThrows(IllegalArgumentException.class, () -> Sha256.sha256Sync((String) null));
        assertThrows(IllegalArgumentException.class, () -> Sha256.sha256Sync((byte[]) null));
    }

    private static byte[] hexToBytes(String s) {
        String hex = s.trim();
        if ((hex.length() & 1) != 0) throw new IllegalArgumentException("Hex length must be even");
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("Invalid hex at index " + (i * 2));
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
