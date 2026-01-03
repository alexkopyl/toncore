import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.tuple.Tuple;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static dev.quark.ton.core.tuple.Tuple.*;
import static org.junit.jupiter.api.Assertions.*;

public class TupleSpecTest {

    private static String toBase64NoIdxNoCrc(Cell c) {
        Cell.SerializeOptions o = new Cell.SerializeOptions();
        o.idx = false;
        o.crc32 = false;
        return Base64.getEncoder().encodeToString(c.toBoc(o));
    }

    private static String toBase64UrlNoPadNoIdxNoCrc(Cell c) {
        Cell.SerializeOptions o = new Cell.SerializeOptions();
        o.idx = false;
        o.crc32 = false;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(c.toBoc(o));
    }

    @Test
    public void shouldSerializeTupleWithNumbers() {
        Cell serialized = Tuple.serializeTuple(List.of(
                new TupleItemInt(BigInteger.valueOf(-1)),
                new TupleItemInt(BigInteger.valueOf(-1)),
                new TupleItemInt(new BigInteger("49800000000")),
                new TupleItemInt(new BigInteger("100000000")),
                new TupleItemInt(new BigInteger("100000000")),
                new TupleItemInt(new BigInteger("2500")),
                new TupleItemInt(new BigInteger("100000000"))
        ));

        String golden = "te6ccgEBCAEAWQABGAAABwEAAAAABfXhAAEBEgEAAAAAAAAJxAIBEgEAAAAABfXhAAMBEgEAAAAABfXhAAQBEgEAAAALmE+yAAUBEgH//////////wYBEgH//////////wcAAA==";
        assertEquals(golden, toBase64NoIdxNoCrc(serialized));
    }

    @Test
    public void shouldSerializeTupleLongNumbers() {
        String golden = "te6ccgEBAgEAKgABSgAAAQIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAqt4e0IsLXV0BAAA=";

        Cell serialized = Tuple.serializeTuple(List.of(
                new TupleItemInt(new BigInteger("12312312312312323421"))
        ));

        assertEquals(golden, toBase64NoIdxNoCrc(serialized));
    }

    @Test
    public void shouldSerializeSlices() {
        String golden = "te6ccgEBAwEAHwACDwAAAQQAB0AgAQIAAAAd4GEghEZ4iF1r9AWzyJs4";

        Cell sliceCell = Builder.beginCell()
                .storeCoins(new BigInteger("123123123123123234211234123123123"))
                .endCell();

        Cell serialized = Tuple.serializeTuple(List.of(new TupleItemSlice(sliceCell)));

        assertEquals(golden, toBase64NoIdxNoCrc(serialized));
    }

    @Test
    public void shouldSerializeAddress() {
        String golden = "te6ccgEBAwEAMgACDwAAAQQAELAgAQIAAABDn_k3QjSzAxvCFAxl2WAXIYvKOdG_BD9NlNG8vx1vw1C00A";

        Address a = Address.parse("kf_JuhGlmBjeEKBjLssAuQxeUc6N-CH6bKaN5fjrfhqFpqVQ");
        Cell sliceCell = Builder.beginCell().storeAddress(a).endCell();

        Cell serialized = Tuple.serializeTuple(List.of(new TupleItemSlice(sliceCell)));

        assertEquals(golden, toBase64UrlNoPadNoIdxNoCrc(serialized));
    }

    @Test
    public void shouldSerializeIntHuge() {
        String golden = "te6ccgEBAgEAKgABSgAAAQIAyboRpZgY3hCgYy7LALkMXlHOjfgh+mymjeX4634ahaYBAAA=";

        Cell serialized = Tuple.serializeTuple(List.of(
                new TupleItemInt(new BigInteger(
                        "91243637913382117273357363328745502088904016167292989471764554225637796775334"
                ))
        ));

        assertEquals(golden, toBase64NoIdxNoCrc(serialized));
    }

    @Test
    public void shouldSerializeTuples_RoundtripGolden() {
        String golden =
                "te6ccgEBEAEAjgADDAAABwcABAEIDQESAf//////////AgESAQAAAAAAAAADAwESAQAAAAAAAAACBAESAQAAAAAAAAABBQECAAYBEgEAAAAAAAAAAQcAAAIACQwCAAoLABIBAAAAAAAAAHsAEgEAAAAAAAHimQECAw8BBgcAAQ4BCQQAB0AgDwAd4GEghEZ4iF1r9AWzyJs4";

        Cell root = Cell.fromBoc(Base64.getDecoder().decode(golden)).get(0);

        List<TupleItem> parsed = Tuple.parseTuple(root);
        Cell reserialized = Tuple.serializeTuple(parsed);

        assertEquals(golden, toBase64NoIdxNoCrc(reserialized));
    }

    private static byte[] decodeBase64Flexible(String s) {
        String cleaned = s.replaceAll("\\s+", ""); // убираем переносы/пробелы
        boolean isUrl = cleaned.indexOf('-') >= 0 || cleaned.indexOf('_') >= 0;
        return (isUrl ? Base64.getUrlDecoder() : Base64.getDecoder()).decode(cleaned);
    }

    private static String readUtf8Resource(String path) throws Exception {
        try (var is = TupleSpecTest.class.getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Missing resource " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void shouldParseLargeTupleFromEmulator_DoesNotThrow() throws Exception {
        String b64 = readUtf8Resource("/tuple/large_tuple_from_emulator.b64");
        byte[] boc = decodeBase64Flexible(b64);

        Cell cell = Cell.fromBoc(boc).get(0);
        assertDoesNotThrow(() -> parseTuple(cell));
    }
}
