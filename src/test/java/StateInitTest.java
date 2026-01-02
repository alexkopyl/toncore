import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.StateInit;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class StateInitTest {

    @Test
    void shouldSerializeToMatchGolden1_andParseBack() {
        // Arrange: code/data
        Cell code = Builder.beginCell().storeUint(1, 8).endCell();
        Cell data = Builder.beginCell().storeUint(2, 8).endCell();

        StateInit si = new StateInit(
                null,   // splitDepth
                null,   // special (TickTock)
                code,
                data,
                null    // libraries
        );

        Cell root = Builder.beginCell()
                .store(StateInit.storeStateInit(si))
                .endCell();

        // toBoc({ idx: false, crc32: true })
        Cell.SerializeOptions opts = new Cell.SerializeOptions();
        opts.idx = false;
        opts.crc32 = true;

        byte[] boc = root.toBoc(opts);
        String b64 = Base64.getEncoder().encodeToString(boc);

        // Assert: golden
        assertEquals("te6cckEBAwEACwACATQBAgACAQACAoN/wQo=", b64);

        // Parse
        Cell parsedRoot = Cell.fromBoc(boc).get(0);
        StateInit parsed = StateInit.loadStateInit(parsedRoot.beginParse());

        // Assert parsed fields
        assertNull(parsed.libraries(), "libraries must be null/absent");
        assertNull(parsed.special(), "special must be null/absent");
        assertNull(parsed.splitDepth(), "splitDepth must be null/absent");

        assertNotNull(parsed.code(), "code must be present");
        assertNotNull(parsed.data(), "data must be present");

        long a = parsed.code().beginParse().loadUint(8);
        long b = parsed.data().beginParse().loadUint(8);

        assertEquals(1L, a);
        assertEquals(2L, b);

        // Roundtrip: store(parsed) must match original cell
        Cell root2 = Builder.beginCell()
                .store(StateInit.storeStateInit(parsed))
                .endCell();

        assertTrue(root.equals(root2), "Stored cell must equal after parse->store roundtrip");
    }
}
