import dev.quark.ton.core.boc.BitReader;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.CellType;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.Dictionary;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DictionaryTest {

    private static Builder storeBits(Builder builder, String src) {
        for (int i = 0; i < src.length(); i++) {
            char s = src.charAt(i);
            if (s == '0') builder.storeBit(false);
            else if (s == '1') builder.storeBit(true);
            else throw new IllegalArgumentException("Invalid bit char: " + s);
        }
        return builder;
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

    private static byte[] readResourceBytes(String path) {
        try (InputStream is = DictionaryTest.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String readResourceString(String path) {
        try (InputStream is = DictionaryTest.class.getResourceAsStream(path)) {
            if (is == null) return null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // === Merkle proof/update parsers (аналог exoticMerkleProof/exoticMerkleUpdate для нужных полей) ===
    // Предполагаемый layout (тон-core совместимый):
    // MerkleProof:  [8 bits type][256 bits proofHash][16 bits proofDepth]...
    // MerkleUpdate: [8 bits type][256 bits proofHash1][256 bits proofHash2][16 bits depth1][16 bits depth2]...
    private static byte[] parseMerkleProofHash(Cell proof) {
        BitReader r = new BitReader(proof.bits);
        r.loadUint(8); // type tag
        return r.loadBuffer(32);
    }

    private static byte[] parseMerkleUpdateProofHash1(Cell update) {
        BitReader r = new BitReader(update.bits);
        r.loadUint(8); // type tag
        return r.loadBuffer(32);
    }

    @Test
    void shouldParseAndSerializeDictFromExample() {
        Cell root =
                storeBits(Builder.beginCell(), "11001000")
                        .storeRef(
                                storeBits(Builder.beginCell(), "011000")
                                        .storeRef(storeBits(Builder.beginCell(), "1010011010000000010101001").endCell())
                                        .storeRef(storeBits(Builder.beginCell(), "1010000010000000100100001").endCell())
                                        .endCell()
                        )
                        .storeRef(storeBits(Builder.beginCell(), "1011111011111101111100100001").endCell())
                        .endCell();

        // Unpack
        Dictionary<Long, Long> dict = Dictionary.loadDirect(Dictionary.Keys.Uint(16), Dictionary.Values.Uint(16), root.beginParse());
        assertEquals(169L, dict.get(13L));
        assertEquals(289L, dict.get(17L));
        assertEquals(57121L, dict.get(239L));

        // Empty (без сериализаторов в объекте, как в TS Dictionary.empty<number,number>())
        Dictionary<Long, Long> fromEmpty = Dictionary.empty();
        fromEmpty.set(13L, 169L);
        fromEmpty.set(17L, 289L);
        fromEmpty.set(239L, 57121L);

        // Pack
        Cell packed = Builder.beginCell().storeDictDirect(dict, Dictionary.Keys.Uint(16), Dictionary.Values.Uint(16)).endCell();
        Cell packed2 = Builder.beginCell()
                .storeDictDirect(fromEmpty, Dictionary.Keys.Uint(16), Dictionary.Values.Uint(16))
                .endCell();

        // Compare
        assertTrue(packed.equals(root));
        assertTrue(packed2.equals(root));
    }

    @Test
    void shouldCorrectlySerializeBitStringKeysAndValues() {
        int keyLen = 9; // Not 8 bit aligned
        var keys = Dictionary.Keys.BitString(keyLen);
        var values = Dictionary.Values.BitString(72);

        BitString testKey = new BitString("Test".getBytes(StandardCharsets.UTF_8), 0, keyLen);
        BitString testVal = new BitString("BitString".getBytes(StandardCharsets.UTF_8), 0, 72);

        Dictionary<BitString, BitString> testDict = Dictionary.empty(keys, values);
        testDict.set(testKey, testVal);

        assertNotNull(testDict.get(testKey));
        assertTrue(testDict.get(testKey).equalsBits(testVal));

        Cell serialized = Builder.beginCell().storeDictDirect(testDict, keys, values).endCell();
        Dictionary<BitString, BitString> dictDs = Dictionary.loadDirect(keys, values, serialized);

        assertNotNull(dictDs.get(testKey));
        assertTrue(dictDs.get(testKey).equalsBits(testVal));
    }

    @Test
    void shouldGenerateMerkleProofs() {
        Dictionary<Long, Long> d = Dictionary.empty(
                Dictionary.Keys.Uint(8),
                Dictionary.Values.Uint(32)
        );
        d.set(1L, 11L);
        d.set(2L, 22L);
        d.set(3L, 33L);
        d.set(4L, 44L);
        d.set(5L, 55L);

        byte[] expected = hexToBytes("ee41b86bd71f8224ebd01848b4daf4cd46d3bfb3e119d8b865ce7c2802511de3");

        for (long k = 1; k <= 5; k++) {
            Cell proof = d.generateMerkleProof(k);

            // roundtrip boc
            assertDoesNotThrow(() -> Cell.fromBoc(proof.toBoc()));

            assertTrue(proof.isExotic());
            assertEquals(CellType.MerkleProof, proof.type);

            // proofHash как в спеках
            assertArrayEquals(expected, parseMerkleProofHash(proof));
        }
    }

    @Test
    void shouldGenerateMerkleUpdates() {
        Dictionary<Long, Long> d = Dictionary.empty(
                Dictionary.Keys.Uint(8),
                Dictionary.Values.Uint(32)
        );
        d.set(1L, 11L);
        d.set(2L, 22L);
        d.set(3L, 33L);
        d.set(4L, 44L);
        d.set(5L, 55L);

        byte[] expected = hexToBytes("ee41b86bd71f8224ebd01848b4daf4cd46d3bfb3e119d8b865ce7c2802511de3");

        for (long k = 1; k <= 5; k++) {
            long cur = d.get(k);
            Cell update = d.generateMerkleUpdate(k, cur * 2);

            assertDoesNotThrow(() -> Cell.fromBoc(update.toBoc()));

            assertTrue(update.isExotic());
            assertEquals(CellType.MerkleUpdate, update.type);

            assertArrayEquals(expected, parseMerkleUpdateProofHash1(update));

            // TS: d.set(k, Math.floor(d.get(k)! / 2));
            d.set(k, d.get(k) / 2);
        }
    }

    @Test
    void shouldParseConfig_ifTestdataPresent() {
        // TS читает __testdata__/config.txt как base64 строку
        String base64 = readResourceString("/__testdata__/config.txt");
        assumeTrue(base64 != null && !base64.isBlank(), "config.txt not found in test resources");

        byte[] boc = Base64.getDecoder().decode(base64);
        Cell cell = Cell.fromBoc(boc).get(0);

        // cell.beginParse().loadDictDirect(Dictionary.Keys.Int(32), Dictionary.Values.Cell())
        Dictionary<Long, Cell> configs = cell.beginParse().loadDictDirect(Dictionary.Keys.Int(32), Dictionary.Values.Cell());

        long[] ids = new long[]{0, 1, 2, 4, 7, 8, 9, 10, 11, 12, 14, 15, 16, 17, 18, 20, 21, 22, 23, 24, 25, 28, 29, 31, 32, 34, 71, 72, -999, -71};

        List<Long> keys = configs.keys();
        for (long id : ids) {
            assertTrue(keys.contains(id), "Expected configs to contain key " + id);
            assertNotNull(configs.get(id), "Expected config value for key " + id);
            assertTrue(configs.has(id));
        }
    }

    @Test
    void shouldParseBridgeConfig_ifTestdataPresent() {
        String base64 = readResourceString("/__testdata__/config.txt");
        assumeTrue(base64 != null && !base64.isBlank(), "config.txt not found in test resources");

        byte[] boc = Base64.getDecoder().decode(base64);
        Cell cell = Cell.fromBoc(boc).get(0);

        Dictionary<Long, Cell> configs = cell.beginParse().loadDictDirect(Dictionary.Keys.Int(32), Dictionary.Values.Cell());

        for (long id : new long[]{71, 72}) {
            Cell r = configs.get(id);
            assertNotNull(r);

            Slice config = r.beginParse();
            byte[] bridgeAddress = config.loadBuffer(32);
            byte[] oracleMultisigAddress = config.loadBuffer(32);
            Dictionary<BigInteger, byte[]> oracles = config.loadDict(Dictionary.Keys.BigUint(256), Dictionary.Values.Buffer(32));
            byte[] externalChainAddress = config.loadBuffer(32);

            assertEquals(32, bridgeAddress.length);
            assertEquals(32, oracleMultisigAddress.length);
            assertNotNull(oracles);
            assertEquals(32, externalChainAddress.length);
        }
    }

    @Test
    void shouldParseDictionaryWithEmptyValues_ifTestdataPresent() {
        byte[] boc = readResourceBytes("/__testdata__/empty_value.boc");
        assumeTrue(boc != null && boc.length > 0, "empty_value.boc not found in test resources");

        Cell cell = Cell.fromBoc(boc).get(0);

        Dictionary<BigInteger, BitString> testDict =
                Dictionary.loadDirect(Dictionary.Keys.BigUint(256), Dictionary.Values.BitString(0), cell);

        assertEquals(1, testDict.keys().size());
        assertEquals(BigInteger.valueOf(123), testDict.keys().get(0));
        BitString v = testDict.get(BigInteger.valueOf(123));
        assertNotNull(v);
        assertEquals(0, v.length());
    }
}
