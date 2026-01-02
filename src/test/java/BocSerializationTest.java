import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.cell.serialization.BocSerialization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BocSerializationTest {

    // Вектора "wallet code" из serialization.spec.ts
    private static final String[] WALLETS_HEX = new String[]{
            "B5EE9C72410101010044000084FF0020DDA4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED5441FDF089",
            "B5EE9C724101010100530000A2FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED54D0E2786F",
            "B5EE9C7241010101005F0000BAFF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED54B5B86E42",
            "B5EE9C724101010100570000AAFF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54A1370BB6",
            "B5EE9C724101010100630000C2FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54044CD7A1",
            "B5EE9C724101010100620000C0FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED543FBE6EE0",
            "B5EE9C724101010100710000DEFF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED5410BD6DAD"
    };

    @Test
    void shouldParseWalletCode_Roundtrip() {
        for (String w : WALLETS_HEX) {
            byte[] src = hexToBytes(w);
            Cell c = BocSerialization.deserializeBoc(src).get(0);

            byte[] b = BocSerialization.serializeBoc(c, false, true);
            Cell c2 = BocSerialization.deserializeBoc(b).get(0);

            // Не зависим от equals()/hash(): сравниваем канонический BOC, полученный из c2
            byte[] b2 = BocSerialization.serializeBoc(c2, false, true);
            assertArrayEquals(b, b2);
        }
    }

    @Test
    void shouldSerializeSingleCellWithEmptyBits() {
        Cell cell = Builder.beginCell().endCell();
        assertEquals("x{}", cell.toString());

        assertEquals("te6ccgEBAQEAAgAAAA==", base64(BocSerialization.serializeBoc(cell, false, false)));
        assertEquals("te6cckEBAQEAAgAAAEysuc0=", base64(BocSerialization.serializeBoc(cell, false, true)));
        assertEquals("te6ccoEBAQEAAgACAAA=", base64(BocSerialization.serializeBoc(cell, true, false)));
        assertEquals("te6ccsEBAQEAAgACAAC4Afhr", base64(BocSerialization.serializeBoc(cell, true, true)));

        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccgEBAQEAAgAAAA==")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6cckEBAQEAAgAAAEysuc0=")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccoEBAQEAAgACAAA=")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccsEBAQEAAgACAAC4Afhr")).get(0).toString());
    }

    @Test
    void shouldSerializeSingleCellWithByteAlignedBits() {
        Cell cell = Builder.beginCell().storeUint(123456789L, 32).endCell();
        assertEquals("x{075BCD15}", cell.toString());

        assertEquals("te6ccgEBAQEABgAACAdbzRU=", base64(BocSerialization.serializeBoc(cell, false, false)));
        assertEquals("te6cckEBAQEABgAACAdbzRVRblCS", base64(BocSerialization.serializeBoc(cell, false, true)));
        assertEquals("te6ccoEBAQEABgAGAAgHW80V", base64(BocSerialization.serializeBoc(cell, true, false)));
        assertEquals("te6ccsEBAQEABgAGAAgHW80ViGH1dQ==", base64(BocSerialization.serializeBoc(cell, true, true)));

        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccgEBAQEABgAACAdbzRU=")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6cckEBAQEABgAACAdbzRVRblCS")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccoEBAQEABgAGAAgHW80V")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccsEBAQEABgAGAAgHW80ViGH1dQ==")).get(0).toString());
    }

    @Test
    void shouldSerializeSingleCellWithNonAlignedBits() {
        Cell cell = Builder.beginCell().storeUint(123456789L, 34).endCell();
        assertEquals("x{01D6F3456_}", cell.toString());

        assertEquals("te6ccgEBAQEABwAACQHW80Vg", base64(BocSerialization.serializeBoc(cell, false, false)));
        assertEquals("te6cckEBAQEABwAACQHW80Vgb11ZoQ==", base64(BocSerialization.serializeBoc(cell, false, true)));
        assertEquals("te6ccoEBAQEABwAHAAkB1vNFYA==", base64(BocSerialization.serializeBoc(cell, true, false)));
        assertEquals("te6ccsEBAQEABwAHAAkB1vNFYM0Si3w=", base64(BocSerialization.serializeBoc(cell, true, true)));

        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccgEBAQEABwAACQHW80Vg")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6cckEBAQEABwAACQHW80Vgb11ZoQ==")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccoEBAQEABwAHAAkB1vNFYA==")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccsEBAQEABwAHAAkB1vNFYM0Si3w=")).get(0).toString());
    }

    @Test
    void shouldSerializeSingleCellWithSingleReference() {
        Cell refCell = Builder.beginCell().storeUint(123456789L, 32).endCell();
        Cell cell = Builder.beginCell().storeUint(987654321L, 32).storeRef(refCell).endCell();

        assertEquals("x{3ADE68B1}\n x{075BCD15}", cell.toString());

        assertEquals("te6ccgEBAgEADQABCDreaLEBAAgHW80V", base64(BocSerialization.serializeBoc(cell, false, false)));
        assertEquals("te6cckEBAgEADQABCDreaLEBAAgHW80VSW/75w==", base64(BocSerialization.serializeBoc(cell, false, true)));
        assertEquals("te6ccoEBAgEADQAHDQEIOt5osQEACAdbzRU=", base64(BocSerialization.serializeBoc(cell, true, false)));
        assertEquals("te6ccsEBAgEADQAHDQEIOt5osQEACAdbzRUxP4cd", base64(BocSerialization.serializeBoc(cell, true, true)));


        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccgEBAgEADQABCDreaLEBAAgHW80V")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6cckEBAgEADQABCDreaLEBAAgHW80VSW/75w==")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccoEBAgEADQAABwEIOt5osQEACAdbzRU=")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccsEBAgEADQAHDQEIOt5osQEACAdbzRUxP4cd")).get(0).toString());
    }

    @Test
    void shouldSerializeSingleCellWithMultipleReferences() {
        Cell refCell = Builder.beginCell().storeUint(123456789L, 32).endCell();
        Cell cell = Builder.beginCell()
                .storeUint(987654321L, 32)
                .storeRef(refCell)
                .storeRef(refCell)
                .storeRef(refCell)
                .endCell();

        assertEquals("x{3ADE68B1}\n x{075BCD15}\n x{075BCD15}\n x{075BCD15}", cell.toString());

        assertEquals("te6ccgEBAgEADwADCDreaLEBAQEACAdbzRU=", base64(BocSerialization.serializeBoc(cell, false, false)));
        assertEquals("te6cckEBAgEADwADCDreaLEBAQEACAdbzRWpQD2p", base64(BocSerialization.serializeBoc(cell, false, true)));
        assertEquals("te6ccoEBAgEADwAJDwMIOt5osQEBAQAIB1vNFQ==", base64(BocSerialization.serializeBoc(cell, true, false)));
        assertEquals("te6ccsEBAgEADwAJDwMIOt5osQEBAQAIB1vNFZz9usI=", base64(BocSerialization.serializeBoc(cell, true, true)));

        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccgEBAgEADwADCDreaLEBAQEACAdbzRU=")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6cckEBAgEADwADCDreaLEBAQEACAdbzRWpQD2p")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccoEBAgEADwAACQMIOt5osQEBAQAIB1vNFQ==")).get(0).toString());
        assertEquals(cell.toString(), BocSerialization.deserializeBoc(b64("te6ccsEBAgEADwAJDwMIOt5osQEBAQAIB1vNFZz9usI=")).get(0).toString());
    }

    @Test
    void shouldDeserializeSerializeLibraryCell_VectorMatches() {
        // Вектор из serialization.spec.ts: "should deserialize/serialize library cell"
        String bocBase64 = "te6ccgEBAgEALQABDv8AiNDtHtgBCEICGbgzd5nhZ9WhSM+4juFCvgMYJOtxthFdtTKIH6M/6SM=";

        Cell cell = BocSerialization.deserializeBoc(b64(bocBase64)).get(0);
        assertEquals(
                "x{FF0088D0ED1ED8}\n x{0219B8337799E167D5A148CFB88EE142BE031824EB71B6115DB532881FA33FE923}",
                cell.toString()
        );

        // Спека сравнивает serializeBoc(cell, { idx:false, crc32:false }) == исходной base64
        assertEquals(bocBase64, base64(BocSerialization.serializeBoc(cell, false, false)));
    }

    // -------------------------
    // Optional tests that require files.
    // Put them in src/test/resources/vectors/serialization/...
    // -------------------------

    @Test
    void shouldParseManyCellsTxt_IfPresent() throws Exception {
        byte[] boc = readBase64ResourceOrSkip("/vectors/serialization/manyCells.txt");
        Cell c = BocSerialization.deserializeBoc(boc).get(0);
        byte[] b = BocSerialization.serializeBoc(c, false, true);
        Cell c2 = BocSerialization.deserializeBoc(b).get(0);
        assertArrayEquals(b, BocSerialization.serializeBoc(c2, false, true));
    }

    @Test
    void shouldParseAccountStateTxt_IfPresent() throws Exception {
        byte[] boc = readBase64ResourceOrSkip("/vectors/serialization/accountState.txt");
        Cell c = BocSerialization.deserializeBoc(boc).get(0);
        byte[] b = BocSerialization.serializeBoc(c, false, true);
        Cell c2 = BocSerialization.deserializeBoc(b).get(0);
        assertArrayEquals(b, BocSerialization.serializeBoc(c2, false, true));
    }

    @Test
    void shouldParseAccountProofTxt_IfPresent() throws Exception {
        byte[] boc = readBase64ResourceOrSkip("/vectors/serialization/accountProof.txt");
        Cell c = BocSerialization.deserializeBoc(boc).get(0);
        byte[] b = BocSerialization.serializeBoc(c, false, true);
        Cell c2 = BocSerialization.deserializeBoc(b).get(0);
        assertArrayEquals(b, BocSerialization.serializeBoc(c2, false, true));
    }

    @Test
    void shouldParseConfigProofTxt_IfPresent() throws Exception {
        byte[] boc = readBase64ResourceOrSkip("/vectors/serialization/configProof.txt");
        Cell c = BocSerialization.deserializeBoc(boc).get(0);
        byte[] b = BocSerialization.serializeBoc(c, false, true);
        Cell c2 = BocSerialization.deserializeBoc(b).get(0);
        assertArrayEquals(b, BocSerialization.serializeBoc(c2, false, true));
    }

    @Test
    void shouldParseAccountStateTestTxt_IfPresent() throws Exception {
        byte[] boc = readBase64ResourceOrSkip("/vectors/serialization/accountStateTest.txt");
        Cell c = BocSerialization.deserializeBoc(boc).get(0);
        byte[] b = BocSerialization.serializeBoc(c, false, true);
        Cell c2 = BocSerialization.deserializeBoc(b).get(0);
        assertArrayEquals(b, BocSerialization.serializeBoc(c2, false, true));
    }

    @Test
    void shouldParseAccountStateTestPrunedTxt_IfPresent() throws Exception {
        byte[] boc = readBase64ResourceOrSkip("/vectors/serialization/accountStateTestPruned.txt");
        Cell c = BocSerialization.deserializeBoc(boc).get(0);
        byte[] b = BocSerialization.serializeBoc(c, false, true);
        Cell c2 = BocSerialization.deserializeBoc(b).get(0);
        assertArrayEquals(b, BocSerialization.serializeBoc(c2, false, true));
    }

    @Test
    void shouldParseLargeBocTxt_IfPresent() throws Exception {
        byte[] boc = readBase64ResourceOrSkip("/vectors/serialization/largeBoc.txt");
        Cell c = BocSerialization.deserializeBoc(boc).get(0);
        assertDoesNotThrow(() -> BocSerialization.serializeBoc(c, false, true));
    }

    @Test
    void shouldParseVeryLargeBoc_IfPresent() throws Exception {
        byte[] boc = readBinaryResourceOrSkip("/vectors/serialization/veryLarge.boc");
        Cell c = BocSerialization.deserializeBoc(boc).get(0);
        byte[] b = BocSerialization.serializeBoc(c, false, true);
        Cell c2 = BocSerialization.deserializeBoc(b).get(0);
        assertArrayEquals(b, BocSerialization.serializeBoc(c2, false, true));
    }

    /* ======================= helpers ======================= */

    private static byte[] readBase64ResourceOrSkip(String classpath) throws Exception {
        String s = readUtf8ResourceOrSkip(classpath);
        return Base64.getDecoder().decode(s.trim());
    }

    private static byte[] readBinaryResourceOrSkip(String classpath) throws Exception {
        InputStream is = BocSerializationTest.class.getResourceAsStream(classpath);
        Assumptions.assumeTrue(is != null, "Missing resource " + classpath);
        try (is) {
            return is.readAllBytes();
        }
    }

    private static String readUtf8ResourceOrSkip(String classpath) throws Exception {
        InputStream is = BocSerializationTest.class.getResourceAsStream(classpath);
        Assumptions.assumeTrue(is != null, "Missing resource " + classpath);
        try (is) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] b64(String b64) {
        return Base64.getDecoder().decode(b64);
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
