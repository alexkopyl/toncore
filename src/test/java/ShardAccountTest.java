import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.ShardAccount;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShardAccountTest {

    @Test
    public void shouldParseTonkiteCell() {
        byte[] boc = Base64.getDecoder().decode(
                "te6cckEBBAEA7wABUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEBAnfACD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqCAkCIGAAAACAAAAAAAAAAGgN4Lazp2QAAE0ACAwCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAjPUU3w="
        );

        Cell cell = Cell.fromBoc(boc).get(0);

        ShardAccount shardAccount = ShardAccount.loadShardAccount(cell.beginParse());

        Cell stored = Builder.beginCell()
                .store(ShardAccount.storeShardAccount(shardAccount))
                .endCell();

        assertTrue(cell.equals(stored));
    }
}
