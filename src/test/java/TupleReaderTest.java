import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.quark.ton.core.tuple.Tuple;
import dev.quark.ton.core.tuple.TupleReader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TupleReaderTest {

    @Test
    public void shouldReadCons() {
        List<Tuple.TupleItem> cons = List.of(
                new Tuple.TupleItemTuple(List.of(
                        new Tuple.TupleItemInt(BigInteger.ONE),
                        new Tuple.TupleItemTuple(List.of(
                                new Tuple.TupleItemInt(BigInteger.valueOf(2)),
                                new Tuple.TupleItemTuple(List.of(
                                        new Tuple.TupleItemInt(BigInteger.valueOf(3)),
                                        new Tuple.TupleItemNull()
                                ))
                        ))
                ))
        );

        TupleReader r = new TupleReader(cons);

        List<Tuple.TupleItem> expected = List.of(
                new Tuple.TupleItemInt(BigInteger.ONE),
                new Tuple.TupleItemInt(BigInteger.valueOf(2)),
                new Tuple.TupleItemInt(BigInteger.valueOf(3))
        );

        assertEquals(expected, r.readLispList());
    }

    @Test
    public void shouldReadUltraDeepCons() throws Exception {
        // положи файл как: src/test/resources/tuple/ultra_deep_cons.json
        InputStream is = TupleReaderTest.class.getResourceAsStream("/tuple/ultra_deep_cons.json");
        assertNotNull(is, "Missing /tuple/ultra_deep_cons.json in test resources");

        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(is);

        // файл содержит JSON массив с одним элементом
        List<Tuple.TupleItem> cons = parseTupleItems(root);

        List<Tuple.TupleItem> expected = new ArrayList<>();
        for (int i = 0; i < 187; i++) {
            if (i != 11 && i != 82 && i != 116 && i != 154) {
                expected.add(new Tuple.TupleItemInt(BigInteger.valueOf(i)));
            }
        }

        assertEquals(expected, new TupleReader(cons).readLispList());
    }

    @Test
    public void shouldRaiseErrorOnNonTupleElementInChain() {
        List<Tuple.TupleItem> cons = List.of(new Tuple.TupleItemInt(BigInteger.ONE));
        TupleReader r = new TupleReader(cons);

        IllegalStateException ex = assertThrows(IllegalStateException.class, r::readLispListDirect);
        assertTrue(ex.getMessage().contains("Lisp list consists only from (any, tuple) elements"));
    }

    @Test
    public void shouldReturnEmptyListIfTupleIsNull() {
        List<Tuple.TupleItem> cons = List.of(new Tuple.TupleItemNull());

        TupleReader r = new TupleReader(cons);
        assertEquals(List.of(), r.readLispList());

        r = new TupleReader(cons);
        assertEquals(List.of(), r.readLispListDirect());
    }

    // ---------------- JSON helpers ----------------

    private static List<Tuple.TupleItem> parseTupleItems(JsonNode root) {
        // root is array with 1 element (the tuple)
        JsonNode arr0 = root.get(0);
        return List.of(parseItem(arr0));
    }

    private static Tuple.TupleItem parseItem(JsonNode n) {
        String type = n.get("type").asText();

        return switch (type) {
            case "null" -> new Tuple.TupleItemNull();
            case "int" -> new Tuple.TupleItemInt(new BigInteger(n.get("value").asText()));
            case "nan" -> new Tuple.TupleItemNaN();
            case "tuple" -> {
                List<Tuple.TupleItem> items = new ArrayList<>();
                for (JsonNode it : n.get("items")) {
                    items.add(parseItem(it));
                }
                yield new Tuple.TupleItemTuple(items);
            }
            default -> throw new IllegalArgumentException("Unsupported item in JSON: " + type);
        };
    }
}
