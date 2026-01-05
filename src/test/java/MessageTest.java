import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageTest {

    @Test
    public void shouldHandleEdgeCaseWithExtraCurrency() {
        String tx =
                "te6cckEBBwEA3QADs2gB7ix8WDhQdzzFOCf6hmZ2Dzw2vFNtbavUArvbhXqqqmEAMpuMhx8zp7O3wqMokkuyFkklKpftc4Dh9_5bvavmCo-UXR6uVOIGMkCwAAAAAAC3GwLLUHl_4AYCAQCA_____________________________________________________________________________________gMBPAUEAwFDoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOAUACAAAAAAAAAANoAAAAAEIDF-r-4Q";

        Cell cell = Cell.fromBase64(tx);
        Message message = Message.loadMessage(cell.beginParse());

        Cell stored = Builder.beginCell()
                .store(Message.storeMessage(message, Message.StoreOptions.none()))
                .endCell();

        assertTrue(stored.equals(cell));
    }
}
