import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Slice;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
}
