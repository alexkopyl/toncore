import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.Message;
import dev.quark.ton.core.types.MessageRelaxed;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class MessageRelaxedTest {

    @Test
    void shouldParseMessageRelaxed_VectorRoundtrip() {
        String stateB64 =
                "te6ccsEBAgEAkQA3kQFoYgBgSQkXjXbkhpC1sju4zUJsLIAoavunKbfNsPFbk9jXL6BfXhAAAAAAAAAAAAAAAAAAAQEAsA+KfqUAAAAAAAAAAEO5rKAIAboVCXedy2J0RCseg4yfdNFtU8/BfiaHVEPkH/ze1W+fABicYUqh1j9Lnqv9ZhECm0XNPaB7/HcwoBb3AJnYYfqByAvrwgCqR2XE";

        Cell cell = Cell.fromBoc(Base64.getDecoder().decode(stateB64)).get(0);

        MessageRelaxed relaxed = MessageRelaxed.loadMessageRelaxed(cell.beginParse());

        Cell stored = Builder.beginCell()
                .store(MessageRelaxed.storeMessageRelaxed(relaxed, Message.StoreOptions.none()))
                .endCell();

        assertTrue(stored.equals(cell));
    }
}
