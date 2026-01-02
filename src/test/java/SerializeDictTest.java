import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.dict.SerializeDict;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SerializeDictTest {

    private static Builder storeBits(Builder builder, String src) {
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '0') builder.storeBit(false);
            else if (c == '1') builder.storeBit(true);
            else throw new IllegalArgumentException("Invalid bit char: " + c);
        }
        return builder;
    }

    @Test
    void shouldBuildPrefixTree_FromDocsVector() {
        // From docs (как в serializeDict.spec.ts)
        Map<BigInteger, BigInteger> map = new LinkedHashMap<>();
        map.put(BigInteger.valueOf(13), BigInteger.valueOf(169));
        map.put(BigInteger.valueOf(17), BigInteger.valueOf(289));
        map.put(BigInteger.valueOf(239), BigInteger.valueOf(57121));

        // Serialize
        Builder b = Builder.beginCell();
        SerializeDict.serializeDict(
                map,
                16,
                (src, cell) -> cell.storeUint(src.longValueExact(), 16),
                b
        );
        Cell root = b.endCell();

        // Expected structure (тот же “dict from docs”, который ты уже использовал в ParseDictTest)
        Cell expected =
                storeBits(Builder.beginCell(), "11001000")
                        .storeRef(
                                storeBits(Builder.beginCell(), "011000")
                                        .storeRef(storeBits(Builder.beginCell(), "1010011010000000010101001").endCell())
                                        .storeRef(storeBits(Builder.beginCell(), "1010000010000000100100001").endCell())
                                        .endCell()
                        )
                        .storeRef(storeBits(Builder.beginCell(), "1011111011111101111100100001").endCell())
                        .endCell();

        // Важно сравнивать по хешу/структуре, а не по toString “снимку”
        assertTrue(root.equals(expected), "serializeDict output must match docs vector cell");
        assertEquals(expected.toString(), root.toString());

    }
}
