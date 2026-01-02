import dev.quark.ton.core.boc.*;
import dev.quark.ton.core.boc.CellType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CellTest {

    // Вектор "library cell" из ton-core serialization.spec.ts (мы его уже использовали в BocSerializationTest)
    // Он стабильно даёт экзотическую Library-ячейку.
    private static final String LIBRARY_CELL_BOC_BASE64 =
            "te6ccgEBAgEALQABDv8AiNDtHtgBCEICGbgzd5nhZ9WhSM+4juFCvgMYJOtxthFdtTKIH6M/6SM=";

    @Test
    void shouldConstruct_Default() {
        Cell cell = new Cell();

        assertEquals(CellType.Ordinary, cell.type);
        assertNotNull(cell.bits);
        assertTrue(cell.bits.equalsBits(BitString.EMPTY), "Default bits should be empty");
        assertNotNull(cell.refs);
        assertEquals(0, cell.refs.size());
        assertFalse(cell.isExotic());

        // Hash/depth/level should be well-defined
        assertNotNull(cell.hash());
        assertEquals(32, cell.hash().length, "Cell hash should be 32 bytes");
        assertTrue(cell.depth() >= 0);
        assertTrue(cell.level() >= 0);

        // beginParse should not throw for ordinary
        assertDoesNotThrow(() -> cell.beginParse());
    }

    @Test
    void shouldFailWhenRefsMoreThan4() {
        List<Cell> refs = List.of(new Cell(), new Cell(), new Cell(), new Cell(), new Cell()); // 5 refs
        Cell.Options o = new Cell.Options();
        o.exotic = false;
        o.bits = BitString.EMPTY;
        o.refs = refs;

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new Cell(o));
        assertTrue(ex.getMessage().toLowerCase().contains("references"));
    }

    @Test
    void shouldFailWhenBitsOverflow() {
        // 1024 bits > 1023 must throw
        BitString tooLong = new BitString(new byte[128], 0, 1024);

        Cell.Options o = new Cell.Options();
        o.exotic = false;
        o.bits = tooLong;
        o.refs = List.of();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new Cell(o));
        assertTrue(ex.getMessage().toLowerCase().contains("bits overflow"));
    }

    @Test
    void shouldBeginParseRejectExoticByDefault() {
        Cell root = Cell.fromBase64(LIBRARY_CELL_BOC_BASE64);

        assertNotNull(root);
        assertFalse(root.isExotic(), "Root cell here is ordinary (it just references library cell)");
        assertEquals(1, root.refs.size(), "Spec vector contains 1 reference");

        Cell lib = root.refs.get(0);
        assertTrue(lib.isExotic(), "Referenced cell must be exotic");
        assertEquals(CellType.Library, lib.type);

        assertThrows(IllegalStateException.class, lib::beginParse,
                "Exotic cells cannot be parsed by default");

        assertDoesNotThrow(() -> {
            Slice s = lib.beginParse(true);
            assertNotNull(s);
        });
    }


    @Test
    void shouldHashDepthLevelBeStableAcrossSerializeRoundtrip() {
        Cell c = Builder.beginCell()
                .storeUint(123456789L, 32)
                .storeRef(Builder.beginCell().storeUint(777, 16).endCell())
                .endCell();

        byte[] boc = c.toBoc(); // default: idx=false, crc32=true
        Cell c2 = Cell.fromBoc(boc).get(0);

        assertTrue(c.equals(c2), "Cells must be equal by hash after roundtrip");
        assertArrayEquals(c.hash(), c2.hash());
        assertEquals(c.depth(), c2.depth());
        assertEquals(c.level(), c2.level());
    }

    @Test
    void shouldEqualsWorkByHash() {
        Cell a = Builder.beginCell().storeUint(1, 1).endCell();
        Cell b = Builder.beginCell().storeUint(1, 1).endCell();
        Cell c = Builder.beginCell().storeUint(0, 1).endCell();

        assertTrue(a.equals(b), "Same content should have same hash");
        assertTrue(b.equals(a));
        assertFalse(a.equals(c), "Different content should have different hash");
    }

    @Test
    void shouldSupportToBocOptions() {
        Cell cell = Builder.beginCell().storeUint(0xDEADBEEFL, 32).endCell();

        Cell.SerializeOptions o1 = new Cell.SerializeOptions();
        o1.idx = false;
        o1.crc32 = false;

        Cell.SerializeOptions o2 = new Cell.SerializeOptions();
        o2.idx = true;
        o2.crc32 = true;

        byte[] boc1 = cell.toBoc(o1);
        byte[] boc2 = cell.toBoc(o2);

        assertNotNull(boc1);
        assertNotNull(boc2);
        assertTrue(boc1.length > 0);
        assertTrue(boc2.length > 0);

        // Both should deserialize to the same logical cell
        Cell r1 = Cell.fromBoc(boc1).get(0);
        Cell r2 = Cell.fromBoc(boc2).get(0);
        assertTrue(cell.equals(r1));
        assertTrue(cell.equals(r2));

        // Different flags обычно дают разные байты (не обязано 100%, но почти всегда так)
        assertFalse(Arrays.equals(boc1, boc2), "Different serialization flags should produce different BOC bytes");
    }

    @Test
    void shouldConvertToSliceAndBuilder() {
        Cell cell = Builder.beginCell()
                .storeUint(123, 16)
                .storeRef(Builder.beginCell().storeUint(456, 16).endCell())
                .endCell();

        // asSlice should parse the same underlying bits
        Slice s = cell.asSlice();
        assertEquals(123, s.loadUint(16));

        // asBuilder should rebuild cell from slice
        Cell rebuilt = cell.asBuilder().endCell();
        assertTrue(cell.equals(rebuilt), "asBuilder().endCell() should rebuild same cell by hash");
    }

    @Test
    void shouldFormatToStringWithRefsAndIndent() {
        Cell ref = Builder.beginCell().storeUint(123456789L, 32).endCell();
        Cell cell = Builder.beginCell().storeUint(987654321L, 32).storeRef(ref).endCell();

        // базовый формат (как в спеках serialization)
        assertEquals("x{3ADE68B1}\n x{075BCD15}", cell.toString());

        // формат с indent
        assertEquals("  x{3ADE68B1}\n   x{075BCD15}", cell.toString("  "));
    }

    @Test
    void shouldCreateViaOrdinaryAndExoticFactories() {
        BitString bits = new BitString(new byte[]{(byte) 0xAA}, 0, 8);
        Cell a = Cell.ordinary(bits, List.of());
        assertEquals(CellType.Ordinary, a.type);
        assertFalse(a.isExotic());

        // exotic example from spec vector (the referenced cell is exotic)
        Cell root = Cell.fromBase64(LIBRARY_CELL_BOC_BASE64);
        Cell lib = root.refs.get(0);

        assertTrue(lib.isExotic());
        assertEquals(CellType.Library, lib.type);
    }

    @Test
    void shouldConstruct_likeTonCoreSpec() {
        Cell cell = new Cell();

        // TS: expect(cell.type).toBe(CellType.Ordinary);
        assertEquals(CellType.Ordinary, cell.type);

        // TS: expect(cell.bits.equals(new BitString(Buffer.alloc(0), 0, 0))).toEqual(true);
        BitString empty = new BitString(new byte[0], 0, 0);
        assertTrue(cell.bits.equalsBits(empty));

        // TS: expect(cell.refs).toEqual([]);
        assertNotNull(cell.refs);
        assertEquals(0, cell.refs.size());
    }


}
