import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.address.ContractAddress;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.StateInit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContractAddressTest {

    @Test
    public void shouldResolveAddressCorrectly() {

        Cell code = Builder.beginCell().storeUint(1, 8).endCell();
        Cell data = Builder.beginCell().storeUint(2, 8).endCell();

        // splitDepth=null, special=null, libraries=null
        StateInit init = new StateInit(null, null, code, data, null);

        Address addr = ContractAddress.contractAddress(0, init);

        assertTrue(addr.equals(Address.parse("EQCSY_vTjwGrlvTvkfwhinJ60T2oiwgGn3U7Tpw24kupIhHz")));
    }
}
