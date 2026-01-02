import dev.quark.ton.core.utils.Base32;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class Base32Test {

    @Test
    public void shouldEncodeKnownVectors() {
        assertEquals("", Base32.base32Encode("".getBytes(StandardCharsets.UTF_8)));

        assertEquals("my", Base32.base32Encode("f".getBytes(StandardCharsets.UTF_8)));
        assertEquals("mzxq", Base32.base32Encode("fo".getBytes(StandardCharsets.UTF_8)));
        assertEquals("mzxw6", Base32.base32Encode("foo".getBytes(StandardCharsets.UTF_8)));
        assertEquals("mzxw6yq", Base32.base32Encode("foob".getBytes(StandardCharsets.UTF_8)));
        assertEquals("mzxw6ytb", Base32.base32Encode("fooba".getBytes(StandardCharsets.UTF_8)));
        assertEquals("mzxw6ytboi", Base32.base32Encode("foobar".getBytes(StandardCharsets.UTF_8)));

        assertEquals("nbswy3dp", Base32.base32Encode("hello".getBytes(StandardCharsets.UTF_8)));

        assertEquals(
                "krugkidrovuwg2zamjzg653oebtg66banj2w24dtebxxmzlseb2gqzjanrqxu6jamrxwo",
                Base32.base32Encode("The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    public void shouldDecodeKnownVectors() {
        assertEquals("", new String(Base32.base32Decode(""), StandardCharsets.UTF_8));

        assertEquals("f", new String(Base32.base32Decode("my"), StandardCharsets.UTF_8));
        assertEquals("fo", new String(Base32.base32Decode("mzxq"), StandardCharsets.UTF_8));
        assertEquals("foo", new String(Base32.base32Decode("mzxw6"), StandardCharsets.UTF_8));
        assertEquals("foob", new String(Base32.base32Decode("mzxw6yq"), StandardCharsets.UTF_8));
        assertEquals("fooba", new String(Base32.base32Decode("mzxw6ytb"), StandardCharsets.UTF_8));
        assertEquals("foobar", new String(Base32.base32Decode("mzxw6ytboi"), StandardCharsets.UTF_8));

        assertEquals("hello", new String(Base32.base32Decode("nbswy3dp"), StandardCharsets.UTF_8));

        assertEquals(
                "The quick brown fox jumps over the lazy dog",
                new String(
                        Base32.base32Decode("krugkidrovuwg2zamjzg653oebtg66banj2w24dtebxxmzlseb2gqzjanrqxu6jamrxwo"),
                        StandardCharsets.UTF_8
                )
        );
    }

    @Test
    public void shouldDecodeUppercaseInput() {
        // decoder делает .toLowerCase(), значит uppercase должен проходить
        assertEquals("foobar", new String(Base32.base32Decode("MZXW6YTBOI"), StandardCharsets.UTF_8));
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
        assertThrows(IllegalArgumentException.class, () -> Base32.base32Decode("ab0"));
        assertThrows(IllegalArgumentException.class, () -> Base32.base32Decode("ab8"));
        assertThrows(IllegalArgumentException.class, () -> Base32.base32Decode("ab="));
    }
}
