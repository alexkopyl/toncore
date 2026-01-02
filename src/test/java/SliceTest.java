import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.BitBuilder;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SliceTest {

    private static Random rng(String seed) {
        // детерминированно и достаточно стабильно для тестов
        return new Random(seed.hashCode() * 1103515245L + 12345L);
    }

    private static long nextLongInRange(Random r, long minInclusive, long maxInclusive) {
        long bound = maxInclusive - minInclusive + 1;
        long v = (r.nextLong() >>> 1) % bound; // без отрицательных
        return minInclusive + v;
    }


    private static Cell cellWithBits(BitString bits) {
        // аналог new Cell({ bits }).beginParse() из TS
        return Cell.ordinary(bits, List.of());
    }

    private static Address testAddress(int workchain, String seed) {
        // делаем "raw" адрес детерминированно: wc:SHA256(seed)
        byte[] h = sha256(seed);
        String raw = workchain + ":" + hex(h);
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

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) {
            sb.append(Character.forDigit((x >>> 4) & 0xF, 16));
            sb.append(Character.forDigit(x & 0xF, 16));
        }
        return sb.toString();
    }

    @Test
    void shouldReadUintsFromSlice() {
        Random prando = rng("test-1");
        for (int i = 0; i < 1000; i++) {
            long a = nextLongInRange(prando, 0, 281_474_976_710_655L); // 2^48-1
            long b = nextLongInRange(prando, 0, 281_474_976_710_655L);

            BitBuilder builder = new BitBuilder();
            builder.writeUint(a, 48);
            builder.writeUint(b, 48);
            BitString bits = builder.build();

            {
                Slice reader = cellWithBits(bits).beginParse();
                assertEquals(a, reader.preloadUint(48));
                assertEquals(a, reader.loadUint(48));
                assertEquals(b, reader.preloadUint(48));
                assertEquals(b, reader.loadUint(48));
            }

            {
                Slice reader = cellWithBits(bits).beginParse();
                assertEquals(a, reader.preloadUintBig(48).longValueExact());
                assertEquals(a, reader.loadUintBig(48).longValueExact());
                assertEquals(b, reader.preloadUintBig(48).longValueExact());
                assertEquals(b, reader.loadUintBig(48).longValueExact());
            }
        }
    }

    @Test
    void shouldReadIntsFromSlice() {
        Random prando = rng("test-2");
        for (int i = 0; i < 1000; i++) {
            long a = nextLongInRange(prando, -281_474_976_710_655L, 281_474_976_710_655L);
            long b = nextLongInRange(prando, -281_474_976_710_655L, 281_474_976_710_655L);

            BitBuilder builder = new BitBuilder();
            builder.writeInt(a, 49);
            builder.writeInt(b, 49);
            BitString bits = builder.build();

            {
                Slice reader = cellWithBits(bits).beginParse();
                assertEquals(a, reader.preloadInt(49));
                assertEquals(a, reader.loadInt(49));
                assertEquals(b, reader.preloadInt(49));
                assertEquals(b, reader.loadInt(49));
            }

            {
                Slice reader = cellWithBits(bits).beginParse();
                assertEquals(a, reader.preloadIntBig(49).longValueExact());
                assertEquals(a, reader.loadIntBig(49).longValueExact());
                assertEquals(b, reader.preloadIntBig(49).longValueExact());
                assertEquals(b, reader.loadIntBig(49).longValueExact());
            }
        }
    }

    @Test
    void shouldReadVarUintsFromSlice() {
        Random prando = rng("test-3");
        for (int i = 0; i < 1000; i++) {
            int sizeBits = prando.nextInt(5) + 4; // 4..8
            long a = nextLongInRange(prando, 0, 281_474_976_710_655L);
            long b = nextLongInRange(prando, 0, 281_474_976_710_655L);

            BitBuilder builder = new BitBuilder();
            builder.writeVarUint(a, sizeBits);
            builder.writeVarUint(b, sizeBits);
            BitString bits = builder.build();

            {
                Slice reader = cellWithBits(bits).beginParse();
                assertEquals(a, reader.preloadVarUint(sizeBits));
                assertEquals(a, reader.loadVarUint(sizeBits));
                assertEquals(b, reader.preloadVarUint(sizeBits));
                assertEquals(b, reader.loadVarUint(sizeBits));
            }

            {
                Slice reader = cellWithBits(bits).beginParse();
                assertEquals(a, reader.preloadVarUintBig(sizeBits).longValueExact());
                assertEquals(a, reader.loadVarUintBig(sizeBits).longValueExact());
                assertEquals(b, reader.preloadVarUintBig(sizeBits).longValueExact());
                assertEquals(b, reader.loadVarUintBig(sizeBits).longValueExact());
            }
        }
    }

    @Test
    void shouldReadVarIntsFromSlice() {
        Random prando = rng("test-4");
        for (int i = 0; i < 1000; i++) {
            int sizeBits = prando.nextInt(5) + 4; // 4..8
            long a = nextLongInRange(prando, -281_474_976_710_655L, 281_474_976_710_655L);
            long b = nextLongInRange(prando, -281_474_976_710_655L, 281_474_976_710_655L);

            BitBuilder builder = new BitBuilder();
            builder.writeVarInt(a, sizeBits);
            builder.writeVarInt(b, sizeBits);
            BitString bits = builder.build();

            {
                Slice reader = cellWithBits(bits).beginParse();
                assertEquals(a, reader.preloadVarInt(sizeBits));
                assertEquals(a, reader.loadVarInt(sizeBits));
                assertEquals(b, reader.preloadVarInt(sizeBits));
                assertEquals(b, reader.loadVarInt(sizeBits));
            }

            {
                Slice reader = cellWithBits(bits).beginParse();
                assertEquals(a, reader.preloadVarIntBig(sizeBits).longValueExact());
                assertEquals(a, reader.loadVarIntBig(sizeBits).longValueExact());
                assertEquals(b, reader.preloadVarIntBig(sizeBits).longValueExact());
                assertEquals(b, reader.loadVarIntBig(sizeBits).longValueExact());
            }
        }
    }

    @Test
    void shouldReadCoinsFromSlice() {
        Random prando = rng("test-5");
        for (int i = 0; i < 1000; i++) {
            long a = nextLongInRange(prando, 0, 281_474_976_710_655L);
            long b = nextLongInRange(prando, 0, 281_474_976_710_655L);

            BitBuilder builder = new BitBuilder();
            builder.writeCoins(a);
            builder.writeCoins(b);
            BitString bits = builder.build();

            Slice reader = cellWithBits(bits).beginParse();
            assertEquals(a, reader.preloadCoins().longValueExact());
            assertEquals(a, reader.loadCoins().longValueExact());
            assertEquals(b, reader.preloadCoins().longValueExact());
            assertEquals(b, reader.loadCoins().longValueExact());
        }
    }

    @Test
    void shouldReadAddressFromSlice() {
        Random prando = rng("test-addr");
        for (int i = 0; i < 1000; i++) {
            Address a = (prando.nextInt(20) == 0) ? testAddress(-1, "test-1-" + i) : null;
            Address b = testAddress(0, "test-2-" + i);

            BitBuilder builder = new BitBuilder();
            builder.writeAddress(a);
            builder.writeAddress(b);
            BitString bits = builder.build();

            Slice reader = cellWithBits(bits).beginParse();
            if (a != null) {
                assertEquals(a.toString(), reader.loadMaybeAddress().toString());
            } else {
                assertNull(reader.loadMaybeAddress());
            }
            assertEquals(b.toString(), reader.loadAddress().toString());
        }
    }
}
