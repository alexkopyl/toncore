import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.boc.utils.Strings;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StringsTest {

    private static final String[] CASES = new String[]{
            "123",
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890",
            "привет мир 👀 привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀привет мир 👀"
    };

    @Test
    void shouldSerializeAndParseStrings() {
        for (String c : CASES) {
            Cell cell = Strings.stringToCell(c);
            assertEquals(c, Strings.readString(cell.beginParse()));
        }
    }

    @Test
    void shouldSerializeAndParseStringWithPaddedSlice() {
        for (String c : CASES) {
            // Аналог comment(c) из ton-core: 32 бита "комментария" + строка хвостом
            Cell cell = Builder.beginCell()
                    .storeUint(0, 32)
                    .storeStringTail(c)
                    .endCell();

            Slice s = cell.beginParse().skip(32);
            assertEquals(c, Strings.readString(s));
        }
    }

    @Test
    void shouldSplitLongStringIntoRefChainAndReadBack() {
        // Делаем строку гарантированно больше 127 байт (1023/8=127)
        String c = "A".repeat(500);

        Cell cell = Strings.stringToCell(c);

        // Должна появиться хотя бы 1 ссылка (иначе тест не проверяет ветку split)
        assertTrue(cell.refs.size() >= 1, "Expected ref chain for long string");

        assertEquals(c, Strings.readString(cell.beginParse()));
    }

    @Test
    void shouldThrowOnNonByteAlignedSlice() {
        // Строка должна читаться только при remBits % 8 == 0
        Cell bad = Builder.beginCell().storeBit(true).endCell();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Strings.readString(bad.beginParse()));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid string length"));
    }

    @Test
    void shouldThrowOnInvalidNumberOfRefs() {
        // remRefs must be 0 or 1 (по твоей реализации)
        Cell r1 = Builder.beginCell().storeUint(1, 8).endCell();
        Cell r2 = Builder.beginCell().storeUint(2, 8).endCell();

        Cell bad = Builder.beginCell()
                .storeUint(0xAA, 8)
                .storeRef(r1)
                .storeRef(r2) // 2 refs => invalid for Strings.readBuffer()
                .endCell();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Strings.readString(bad.beginParse()));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid number of refs"));
    }

    @Test
    void shouldWriteStringAndReadBack() {
        String c = "hello world 👋";
        Builder b = Builder.beginCell();
        Strings.writeString(c, b);
        Cell cell = b.endCell();

        assertEquals(c, Strings.readString(cell.beginParse()));
        assertArrayEquals(c.getBytes(StandardCharsets.UTF_8),
                Strings.readString(cell.beginParse()).getBytes(StandardCharsets.UTF_8));
    }
}
