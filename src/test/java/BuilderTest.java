import dev.quark.ton.core.boc.BitReader;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ported structure from ton-core/src/boc/BitReader.spec.ts,
 * but relies on Cell/Slice/strings/address. We'll enable later.
 */
class BuilderTest {

    @Test
    void shouldReadStringTailsFromBuilder() {
        Builder b = Builder.beginCell();
        b.storeStringRefTail("hello");
        b.storeStringTail("world");

        // When Slice exists:
        Slice sc = b.endCell().beginParse();
        assertEquals("hello", sc.loadStringRefTail());
        assertEquals("world", sc.loadStringTail());
        assertTrue(true);
    }

    @Test
    void shouldReadUintsFromBuilder() {
        Random rnd = new Random(1);
        for (int i = 0; i < 1000; i++) {
            long a = nextLongBounded(rnd, 281474976710655L);
            long b = nextLongBounded(rnd, 281474976710655L);

            Builder builder = Builder.beginCell();
            builder.storeUint(a, 48);
            builder.storeUint(b, 48);

            BitString bits = builder.endCell().bits;
            BitReader reader = new BitReader(bits);

            assertEquals(a, reader.preloadUint(48));
            assertEquals(a, reader.loadUint(48));
            assertEquals(b, reader.preloadUint(48));
            assertEquals(b, reader.loadUint(48));
        }
    }

    @Test
    void shouldReadIntsFromBuilder() {
        Random rnd = new Random(2);
        long bound = 281474976710655L;
        for (int i = 0; i < 1000; i++) {
            long a = nextLongSignedBounded(rnd, bound);
            long b = nextLongSignedBounded(rnd, bound);

            Builder builder = Builder.beginCell();
            builder.storeInt(a, 49);
            builder.storeInt(b, 49);

            BitString bits = builder.endCell().bits;
            BitReader reader = new BitReader(bits);

            assertEquals(a, reader.preloadInt(49));
            assertEquals(a, reader.loadInt(49));
            assertEquals(b, reader.preloadInt(49));
            assertEquals(b, reader.loadInt(49));
        }
    }

    @Test
    void shouldReadVarUintsFromBuilder() {
        Random rnd = new Random(3);
        for (int i = 0; i < 1000; i++) {
            int sizeBits = 4 + rnd.nextInt(5); // 4..8
            long a = nextLongBounded(rnd, 281474976710655L);
            long b = nextLongBounded(rnd, 281474976710655L);

            Builder builder = Builder.beginCell();
            builder.storeVarUint(a, sizeBits);
            builder.storeVarUint(b, sizeBits);

            BitString bits = builder.endCell().bits;
            BitReader reader = new BitReader(bits);

            assertEquals(a, reader.preloadVarUint(sizeBits));
            assertEquals(a, reader.loadVarUint(sizeBits));
            assertEquals(b, reader.preloadVarUint(sizeBits));
            assertEquals(b, reader.loadVarUint(sizeBits));
        }
    }

    @Test
    void shouldReadVarIntsFromBuilder() {
        Random rnd = new Random(4);
        long bound = 281474976710655L;
        for (int i = 0; i < 1000; i++) {
            int sizeBits = 4 + rnd.nextInt(5); // 4..8
            long a = nextLongSignedBounded(rnd, bound);
            long b = nextLongSignedBounded(rnd, bound);

            Builder builder = Builder.beginCell();
            builder.storeVarInt(a, sizeBits);
            builder.storeVarInt(b, sizeBits);

            BitString bits = builder.endCell().bits;
            BitReader reader = new BitReader(bits);

            assertEquals(a, reader.preloadVarInt(sizeBits));
            assertEquals(a, reader.loadVarInt(sizeBits));
            assertEquals(b, reader.preloadVarInt(sizeBits));
            assertEquals(b, reader.loadVarInt(sizeBits));
        }
    }

    @Test
    void shouldReadCoinsFromBuilder() {
        Random rnd = new Random(5);
        for (int i = 0; i < 1000; i++) {
            long a = nextLongBounded(rnd, 281474976710655L);
            long b = nextLongBounded(rnd, 281474976710655L);

            Builder builder = Builder.beginCell();
            builder.storeCoins(a);
            builder.storeCoins(b);

            BitString bits = builder.endCell().bits;
            BitReader reader = new BitReader(bits);

            assertEquals(a, reader.preloadCoins().longValueExact());
            assertEquals(a, reader.loadCoins().longValueExact());
            assertEquals(b, reader.preloadCoins().longValueExact());
            assertEquals(b, reader.loadCoins().longValueExact());
        }
    }

    private static long nextLongBounded(Random rnd, long maxInclusive) {
        // uniform-ish for tests, maxInclusive fits in 48 bits
        long v = rnd.nextLong() >>> 16; // keep 48 bits-ish
        long m = maxInclusive + 1;
        return v % m;
    }

    private static long nextLongSignedBounded(Random rnd, long absMaxInclusive) {
        long v = nextLongBounded(rnd, absMaxInclusive);
        return rnd.nextBoolean() ? v : -v;
    }


}
