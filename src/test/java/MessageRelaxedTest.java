import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.Message;
import dev.quark.ton.core.types.MessageRelaxed;
import org.junit.jupiter.api.Test;
import dev.quark.ton.core.types.CommonMessageInfoRelaxedTLB;

import java.math.BigInteger;
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

    @Test
    void shouldStoreExoticMessageRelaxed_DoesNotThrow() {
        String boc = "te6cckEBBgEApwAJRgMNtncFfUUJSR6XK02Y/bjHpB1pj8VtOlnKAxgDtajfKgACASIFgZABAwIoSAEBN4Yioo+yQnBEkgpN5SV1lnSGuoJhL3ShCi0dcMHbuFcAACIBIAUEAE2/fOtFTZyY8zlmFJ8dch//XZQ4QApiXOGPZXvjFv5j0LSgZ7ckWPAoSAEBr+h0Em3TbCgl+CpPMKKoQskNFu4vLU/8w4Zuaz7PRP8AAOG0rdg=";
        Cell cell = Cell.fromBase64(boc);

        MessageRelaxed relaxed = new MessageRelaxed(
                new CommonMessageInfoRelaxedTLB.ExternalOut(null, null, BigInteger.ZERO, 0L)
                ,
                null,
                cell
        );

        assertDoesNotThrow(() -> {
            Builder.beginCell()
                    .store(MessageRelaxed.storeMessageRelaxed(relaxed, Message.StoreOptions.none()))
                    .endCell();
        });
    }
}
