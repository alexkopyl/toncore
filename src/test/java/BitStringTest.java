
import dev.quark.ton.core.boc.BitBuilder;
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

    @Test
    void shouldProcessMonkeyStrings() {
        String[][] cases = new String[][]{
                {"001110101100111010", "3ACEA_"},
                {"01001", "4C_"},
                {"000000110101101010", "035AA_"},
                {"1000011111100010111110111", "87E2FBC_"},
                {"0111010001110010110", "7472D_"},
                {"", ""},
                {"0101", "5"},
                {"010110111010100011110101011110", "5BA8F57A_"},
                {"00110110001101", "3636_"},
                {"1110100", "E9_"},
                {"010111000110110", "5C6D_"},
                {"01", "6_"},
                {"1000010010100", "84A4_"},
                {"010000010", "414_"},
                {"110011111", "CFC_"},
                {"11000101001101101", "C536C_"},
                {"011100111", "73C_"},
                {"11110011", "F3"},
                {"011001111011111000", "67BE2_"},
                {"10101100000111011111", "AC1DF"},
                {"0100001000101110", "422E"},
                {"000110010011011101", "19376_"},
                {"10111001", "B9"},
                {"011011000101000001001001110000", "6C5049C2_"},
                {"0100011101", "476_"},
                {"01001101000001", "4D06_"},
                {"00010110101", "16B_"},
                {"01011011110", "5BD_"},
                {"1010101010111001011101", "AAB976_"},
                {"00011", "1C_"},
                {"11011111111001111100", "DFE7C"},
                {"1110100100110111001101011111000", "E93735F1_"},
                {"10011110010111100110100000", "9E5E682_"},
                {"00100111110001100111001110", "27C673A_"},
                {"01010111011100000000001110000", "57700384_"},
                {"010000001011111111111000", "40BFF8"},
                {"0011110001111000110101100001", "3C78D61"},
                {"101001011011000010", "A5B0A_"},
                {"1111", "F"},
                {"10101110", "AE"},
                {"1001", "9"},
                {"001010010", "294_"},
                {"110011", "CE_"},
                {"10000000010110", "805A_"},
                {"11000001101000100", "C1A24_"},
                {"1", "C_"},
                {"0100101010000010011101111", "4A8277C_"},
                {"10", "A_"},
                {"1010110110110110110100110010110", "ADB6D32D_"},
                {"010100000000001000111101011001", "50023D66_"},
        };

        for (String[] c : cases) {
            String bits = c[0];
            String expected = c[1];

            BitBuilder builder = new BitBuilder();
            for (int i = 0; i < bits.length(); i++) {
                builder.writeBit(bits.charAt(i) == '1');
            }
            BitString r = builder.build();

            // check bits
            for (int i = 0; i < bits.length(); i++) {
                assertEquals(bits.charAt(i) == '1', r.at(i), "Mismatch at bit " + i + " for " + bits);
            }

            // check canonical string
            assertEquals(expected, r.toString(), "Mismatch for " + bits);
        }
    }
}
