import dev.quark.ton.core.utils.Base32;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class Base32Test {

    @Test
    public void shouldEncodeAndDecode_TonCoreVector() {
        String enc = "fvcqmha5j3ceve35ammfrhqty46rkhi455otydstv66pk2tmf7rl25f3";
        byte[] dec = Base32.base32Decode(enc);

        byte[] expected = hexToBytes(
                "2D45061C1D4EC44A937D0318589E13C73D151D1CEF5D3C0E53AFBCF56A6C2FE2BD74BB"
        );
        assertArrayEquals(expected, dec);

        String encodedBack = Base32.base32Encode(expected);
        assertEquals(enc, encodedBack);
    }

    @Test
    public void shouldDecodeUppercaseInput() {
        String enc = "FVCQMHA5J3CEVE35AMMFRHQTY46RKHI455OTYDSTV66PK2TMF7RL25F3";
        byte[] dec = Base32.base32Decode(enc);
        assertEquals(
                "fvcqmha5j3ceve35ammfrhqty46rkhi455otydstv66pk2tmf7rl25f3",
                Base32.base32Encode(dec)
        );
    }

    @Test
    public void shouldRoundtripBinaryData() {
        byte[] src = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, (byte) 255, (byte) 128};
        String enc = Base32.base32Encode(src);
        byte[] dec = Base32.base32Decode(enc);
        assertTrue(Arrays.equals(src, dec));
    }

    @Test
    public void shouldThrowOnInvalidCharacter() {
        assertThrows(IllegalArgumentException.class, () -> Base32.base32Decode("ab0")); // '0' not in alphabet
        assertThrows(IllegalArgumentException.class, () -> Base32.base32Decode("ab8")); // '8' not in alphabet
        assertThrows(IllegalArgumentException.class, () -> Base32.base32Decode("ab=")); // '=' not in alphabet
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if ((len & 1) != 0) throw new IllegalArgumentException("Odd hex length");
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
    }
}
