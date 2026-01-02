import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.address.ExternalAddress;
import dev.quark.ton.core.boc.BitBuilder;
import dev.quark.ton.core.boc.BitReader;
import dev.quark.ton.core.boc.BitString;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ported from ton-core/src/boc/BitReader.spec.ts
 */
class BitReaderTest {

    /**
     * Deterministic RNG replacement for Prando('test-*').
     * Not byte-for-byte identical to Prando, but deterministic for our porting stage.
     */
    private static Random rng(String seed) {
        return new Random(seed.hashCode());
    }

    private static long nextLongInRange(Random r, long minInclusive, long maxInclusive) {
        // inclusive range
        long bound = maxInclusive - minInclusive + 1;
        long x = (long) (r.nextDouble() * bound);
        return minInclusive + x;
    }

    @Test
    void shouldReadUintsFromBuilder() {
        Random prando = rng("test-1");
        for (int i = 0; i < 1000; i++) {
            long a = nextLongInRange(prando, 0, 281474976710655L);
            long b = nextLongInRange(prando, 0, 281474976710655L);

            BitBuilder builder = new BitBuilder();
            builder.writeUint(a, 48);
            builder.writeUint(b, 48);
            BitString bits = builder.build();

            {
                BitReader reader = new BitReader(bits);
                assertEquals(a, reader.preloadUint(48));
                assertEquals(a, reader.loadUint(48));
                assertEquals(b, reader.preloadUint(48));
                assertEquals(b, reader.loadUint(48));
            }

            {
                BitReader reader = new BitReader(bits);
                assertEquals(a, reader.preloadUintBig(48).longValueExact());
                assertEquals(a, reader.loadUintBig(48).longValueExact());
                assertEquals(b, reader.preloadUintBig(48).longValueExact());
                assertEquals(b, reader.loadUintBig(48).longValueExact());
            }
        }
    }

    @Test
    void shouldReadIntsFromBuilder() {
        Random prando = rng("test-2");
        for (int i = 0; i < 1000; i++) {
            long a = nextLongInRange(prando, -281474976710655L, 281474976710655L);
            long b = nextLongInRange(prando, -281474976710655L, 281474976710655L);

            BitBuilder builder = new BitBuilder();
            builder.writeInt(a, 49);
            builder.writeInt(b, 49);
            BitString bits = builder.build();

            {
                BitReader reader = new BitReader(bits);
                assertEquals(a, reader.preloadInt(49));
                assertEquals(a, reader.loadInt(49));
                assertEquals(b, reader.preloadInt(49));
                assertEquals(b, reader.loadInt(49));
            }

            {
                BitReader reader = new BitReader(bits);
                assertEquals(a, reader.preloadIntBig(49).longValueExact());
                assertEquals(a, reader.loadIntBig(49).longValueExact());
                assertEquals(b, reader.preloadIntBig(49).longValueExact());
                assertEquals(b, reader.loadIntBig(49).longValueExact());
            }
        }
    }

    @Test
    void shouldReadVarUintsFromBuilder() {
        Random prando = rng("test-3");
        for (int i = 0; i < 1000; i++) {
            int sizeBits = (int) nextLongInRange(prando, 4, 8);
            long a = nextLongInRange(prando, 0, 281474976710655L);
            long b = nextLongInRange(prando, 0, 281474976710655L);

            BitBuilder builder = new BitBuilder();
            builder.writeVarUint(a, sizeBits);
            builder.writeVarUint(b, sizeBits);
            BitString bits = builder.build();

            {
                BitReader reader = new BitReader(bits);
                assertEquals(a, reader.preloadVarUint(sizeBits));
                assertEquals(a, reader.loadVarUint(sizeBits));
                assertEquals(b, reader.preloadVarUint(sizeBits));
                assertEquals(b, reader.loadVarUint(sizeBits));
            }

            {
                BitReader reader = new BitReader(bits);
                assertEquals(a, reader.preloadVarUintBig(sizeBits).longValueExact());
                assertEquals(a, reader.loadVarUintBig(sizeBits).longValueExact());
                assertEquals(b, reader.preloadVarUintBig(sizeBits).longValueExact());
                assertEquals(b, reader.loadVarUintBig(sizeBits).longValueExact());
            }
        }
    }

    @Test
    void shouldReadVarIntsFromBuilder() {
        Random prando = rng("test-4");
        for (int i = 0; i < 1000; i++) {
            int sizeBits = (int) nextLongInRange(prando, 4, 8);
            long a = nextLongInRange(prando, -281474976710655L, 281474976710655L);
            long b = nextLongInRange(prando, -281474976710655L, 281474976710655L);

            BitBuilder builder = new BitBuilder();
            builder.writeVarInt(a, sizeBits);
            builder.writeVarInt(b, sizeBits);
            BitString bits = builder.build();

            {
                BitReader reader = new BitReader(bits);
                assertEquals(a, reader.preloadVarInt(sizeBits));
                assertEquals(a, reader.loadVarInt(sizeBits));
                assertEquals(b, reader.preloadVarInt(sizeBits));
                assertEquals(b, reader.loadVarInt(sizeBits));
            }

            {
                BitReader reader = new BitReader(bits);
                assertEquals(a, reader.preloadVarIntBig(sizeBits).longValueExact());
                assertEquals(a, reader.loadVarIntBig(sizeBits).longValueExact());
                assertEquals(b, reader.preloadVarIntBig(sizeBits).longValueExact());
                assertEquals(b, reader.loadVarIntBig(sizeBits).longValueExact());
            }
        }
    }

    @Test
    void shouldReadCoinsFromBuilder() {
        Random prando = rng("test-5");
        for (int i = 0; i < 1000; i++) {
            long a = nextLongInRange(prando, 0, 281474976710655L);
            long b = nextLongInRange(prando, 0, 281474976710655L);

            BitBuilder builder = new BitBuilder();
            builder.writeCoins(a);
            builder.writeCoins(b);
            BitString bits = builder.build();

            BitReader reader = new BitReader(bits);
            assertEquals(BigInteger.valueOf(a), reader.preloadCoins());
            assertEquals(BigInteger.valueOf(a), reader.loadCoins());
            assertEquals(BigInteger.valueOf(b), reader.preloadCoins());
            assertEquals(BigInteger.valueOf(b), reader.loadCoins());
        }
    }

    @Test
    void shouldReadAddressFromBuilder() {
        Address a = Address.parse("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO"); // friendly
        Address b = Address.parseRaw("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3"); // raw

        BitBuilder builder = new BitBuilder();
        builder.writeAddress(a);
        builder.writeAddress(b);
        BitString bits = builder.build();

        BitReader reader = new BitReader(bits);

        Address ra = reader.loadAddress();
        Address rb = reader.loadAddress();

        // Лучше сравнивать по equals (ниже объясню), но пока можно по raw:
        assertEquals(a.toRawString(), ra.toRawString());
        assertEquals(b.toRawString(), rb.toRawString());
    }


    @Test
    void shouldReadExternalAddressFromBuilder() {
        ExternalAddress a = new ExternalAddress(new BigInteger("12345678901234567890"), 96);
        ExternalAddress b = new ExternalAddress(new BigInteger("1"), 1);

        BitBuilder builder = new BitBuilder();
        builder.writeAddress(a);
        builder.writeAddress(b);
        BitString bits = builder.build();

        BitReader reader = new BitReader(bits);

        ExternalAddress ra = reader.loadExternalAddress();
        ExternalAddress rb = reader.loadExternalAddress();

        assertEquals(a, ra);
        assertEquals(b, rb);
    }

    @Test
    void shouldReadAnycastAddress() {
        BitBuilder builder = new BitBuilder();
        builder.writeUint(0b10, 2); // addr_std tag
        builder.writeUint(0b1, 1);  // anycast_info present
        builder.writeUint(2, 5);    // anycast depth
        builder.writeUint(1, 2);    // rewrite_pfx (2 bits)
        builder.writeInt(0, 8);     // workchain_id
        builder.writeUint(BigInteger.ONE, 256); // address hash

        BitString bits = builder.build();
        BitReader reader = new BitReader(bits);

        Address addr = reader.loadAddress();
        String expected = "0:4000000000000000000000000000000000000000000000000000000000000001";
        assertEquals(expected, addr.toRawString());
    }

}
