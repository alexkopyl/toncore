import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.dict.utils.InternalKeySerializer;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class InternalKeySerializerTest {

    // Аналог testAddress(wc, seed) из ton-core: делаем детерминированный raw-адрес на базе sha256(seed)
    private static Address testAddress(int workchain, String seed) {
        byte[] h = sha256(seed);
        String raw = workchain + ":" + toHexLower(h);
        return Address.parseRaw(raw);
    }

    private static byte[] sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static String toHexLower(byte[] data) {
        char[] out = new char[data.length * 2];
        int p = 0;
        for (byte b : data) {
            int v = b & 0xFF;
            out[p++] = HEX[v >>> 4];
            out[p++] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    private static byte[] hexToBytes(String s) {
        String hex = s.trim();
        if ((hex.length() & 1) != 0) throw new IllegalArgumentException("Hex length must be even");
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("Invalid hex");
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    @Test
    void shouldSerializeNumbers() {
        long[] cs = new long[]{0, -1, 1, 123123123L, -123123123L};

        for (long c : cs) {
            String enc = InternalKeySerializer.serializeInternalKey(c);
            Object dec = InternalKeySerializer.deserializeInternalKey(enc);

            assertTrue(dec instanceof Long, "Expected Long for n:");
            assertEquals(c, (long) dec);
        }
    }

    @Test
    void shouldSerializeBigNumbers() {
        BigInteger[] cs = new BigInteger[]{
                BigInteger.ZERO,
                BigInteger.valueOf(-1),
                BigInteger.ONE,
                BigInteger.valueOf(123123123),
                BigInteger.valueOf(-123123123),
                new BigInteger("1231231231231237812683128376123"),
                new BigInteger("-1231273612873681263871263871263")
        };

        for (BigInteger c : cs) {
            String enc = InternalKeySerializer.serializeInternalKey(c);
            Object dec = InternalKeySerializer.deserializeInternalKey(enc);

            assertTrue(dec instanceof BigInteger, "Expected BigInteger for b:");
            assertEquals(c, dec);
        }
    }

    @Test
    void shouldSerializeAddresses() {
        Address[] cs = new Address[]{
                testAddress(0, "1"),
                testAddress(-1, "1"),
                testAddress(0, "2"),
                testAddress(0, "4")
        };

        for (Address c : cs) {
            String enc = InternalKeySerializer.serializeInternalKey(c);
            Object dec = InternalKeySerializer.deserializeInternalKey(enc);

            assertTrue(dec instanceof Address, "Expected Address for a:");
            Address a = (Address) dec;

            // В твоём Address есть equals(Address) (не equals(Object)), поэтому сравниваем так
            assertTrue(a.equals(c), "Decoded address should equal original");
        }
    }

    @Test
    void shouldSerializeBuffers() {
        byte[][] cs = new byte[][]{
                hexToBytes("00"),
                hexToBytes("ff"),
                hexToBytes("0f"),
                hexToBytes("0f000011002233456611")
        };

        for (byte[] c : cs) {
            String enc = InternalKeySerializer.serializeInternalKey(c);
            Object dec = InternalKeySerializer.deserializeInternalKey(enc);

            assertTrue(dec instanceof byte[], "Expected byte[] for f:");
            assertArrayEquals(c, (byte[]) dec);
        }
    }

    @Test
    void shouldSerializeBitStrings_AllBitLengths() {
        byte[][] cs = new byte[][]{
                hexToBytes("00"),
                hexToBytes("ff"),
                hexToBytes("0f"),
                hexToBytes("0f000011002233456611")
        };

        for (byte[] c : cs) {
            int totalBits = c.length * 8;
            for (int i = 0; i < totalBits - 1; i++) {
                BitString bs = new BitString(c, 0, totalBits - i);

                String enc = InternalKeySerializer.serializeInternalKey(bs);
                Object dec = InternalKeySerializer.deserializeInternalKey(enc);

                assertTrue(dec instanceof BitString, "Expected BitString for B:");
                BitString out = (BitString) dec;

                assertTrue(out.equalsBits(bs), "Decoded BitString should equal original");
            }
        }
    }

    @Test
    void shouldRejectUnsupportedType() {
        assertThrows(IllegalArgumentException.class, () -> InternalKeySerializer.serializeInternalKey(new Object()));
    }

    @Test
    void shouldRejectInvalidPrefix() {
        assertThrows(IllegalArgumentException.class, () -> InternalKeySerializer.deserializeInternalKey("x:123"));
        assertThrows(IllegalArgumentException.class, () -> InternalKeySerializer.deserializeInternalKey(""));
        assertThrows(IllegalArgumentException.class, () -> InternalKeySerializer.deserializeInternalKey(null));
    }
}
