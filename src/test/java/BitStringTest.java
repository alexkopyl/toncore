
import dev.quark.ton.core.boc.BitString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BitStringTest {

    private static void assertOobEndsWith(BitString bs, String method, int offset, int length) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            if ("substring".equals(method)) {
                bs.substring(offset, length);
            } else if ("subbuffer".equals(method)) {
                bs.subbuffer(offset, length);
            } else {
                throw new IllegalStateException("Unknown method: " + method);
            }
        });
        assertTrue(ex.getMessage().endsWith("out of bounds"), "Message must end with 'out of bounds' but was: " + ex.getMessage());
    }

    @Test
    void shouldReadBits() {
        BitString bs = new BitString(new byte[]{(byte) 0b10101010}, 0, 8);
        assertTrue(bs.at(0));
        assertFalse(bs.at(1));
        assertTrue(bs.at(2));
        assertFalse(bs.at(3));
        assertTrue(bs.at(4));
        assertFalse(bs.at(5));
        assertTrue(bs.at(6));
        assertFalse(bs.at(7));
        assertEquals("AA", bs.toString());
    }

    @Test
    void shouldEquals() {
        BitString a = new BitString(new byte[]{(byte) 0b10101010}, 0, 8);
        BitString b = new BitString(new byte[]{(byte) 0b10101010}, 0, 8);
        BitString c = new BitString(new byte[]{0, (byte) 0b10101010}, 8, 8);

        assertTrue(a.equalsBits(b));
        assertTrue(b.equalsBits(a));
        assertTrue(a.equalsBits(c));
        assertTrue(c.equalsBits(a));

        assertEquals("AA", a.toString());
        assertEquals("AA", b.toString());
        assertEquals("AA", c.toString());
    }

    @Test
    void shouldFormatStrings() {
        assertEquals("4_", new BitString(new byte[]{(byte) 0b00000000}, 0, 1).toString());
        assertEquals("C_", new BitString(new byte[]{(byte) 0b10000000}, 0, 1).toString());
        assertEquals("E_", new BitString(new byte[]{(byte) 0b11000000}, 0, 2).toString());
        assertEquals("F_", new BitString(new byte[]{(byte) 0b11100000}, 0, 3).toString());
        assertEquals("E",  new BitString(new byte[]{(byte) 0b11100000}, 0, 4).toString());
        assertEquals("EC_", new BitString(new byte[]{(byte) 0b11101000}, 0, 5).toString());
    }

    @Test
    void shouldDoSubbuffers() {
        BitString bs = new BitString(new byte[]{1,2,3,4,5,6,7,8}, 0, 64);
        byte[] b = bs.subbuffer(0, 16);
        assertNotNull(b);
        assertEquals(2, b.length);
    }

    @Test
    void shouldDoSubstrings() {
        BitString bs = new BitString(new byte[]{1,2,3,4,5,6,7,8}, 0, 64);
        BitString b = bs.substring(0, 16);
        assertEquals(16, b.length());
    }

    @Test
    void shouldDoEmptySubstringsWithRequestedLength0() {
        BitString bs = new BitString(new byte[]{1,2,3,4,5,6,7,8}, 0, 64);
        BitString b = bs.substring(bs.length(), 0);
        assertEquals(0, b.length());
    }

    @Test
    void shouldOobWhenSubstringOffsetIsOutOfBounds() {
        BitString bs = new BitString(new byte[]{1,2,3,4,5,6,7,8}, 0, 64);
        assertOobEndsWith(bs, "substring", bs.length() + 1, 0);
        assertOobEndsWith(bs, "substring", -1, 0);
    }

    @Test
    void shouldOobWhenSubbufferOffsetIsOutOfBounds() {
        BitString bs = new BitString(new byte[]{1,2,3,4,5,6,7,8}, 0, 64);
        assertOobEndsWith(bs, "subbuffer", bs.length() + 1, 0);
        assertOobEndsWith(bs, "subbuffer", -1, 0);
    }

    @Test
    void shouldOobWhenOffsetOnEndOfBitstringAndLengthGt0_Substring() {
        BitString bs = new BitString(new byte[]{1,2,3,4,5,6,7,8}, 0, 64);
        assertOobEndsWith(bs, "substring", bs.length(), 1);
    }

    @Test
    void shouldDoEmptySubbuffersWithRequestedLength0() {
        BitString bs = new BitString(new byte[]{1,2,3,4,5,6,7,8}, 0, 64);
        byte[] b = bs.subbuffer(bs.length(), 0);
        assertNotNull(b);
        assertEquals(0, b.length);
    }

    @Test
    void shouldOobWhenOffsetOnEndOfBufferAndLengthGt0_Subbuffer() {
        BitString bs = new BitString(new byte[]{1,2,3,4,5,6,7,8}, 0, 64);
        assertOobEndsWith(bs, "subbuffer", bs.length(), 1);
    }
}
