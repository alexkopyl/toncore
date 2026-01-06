import dev.quark.ton.core.utils.GetMethodId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GetMethodIdTest {

    @Test
    void shouldMatchKnownVectors() {
        assertEquals(0x14C97, GetMethodId.getMethodId("seqno"));
        assertEquals(0x1339C, GetMethodId.getMethodId("get_public_key"));
        assertEquals(0x196C0, GetMethodId.getMethodId("balance"));
        assertEquals(0x16E40, GetMethodId.getMethodId("recv_internal"));
        assertEquals(0x1E9A9, GetMethodId.getMethodId("recv_external"));
        assertEquals(0x17B02, GetMethodId.getMethodId("get_wallet_data"));
        assertEquals(0x1302F, GetMethodId.getMethodId("get_seqno"));
        assertEquals(0x18FCF, GetMethodId.getMethodId("get_nft_data"));
    }

    @Test
    void shouldBeCaseSensitive() {
        assertNotEquals(
                GetMethodId.getMethodId("seqno"),
                GetMethodId.getMethodId("Seqno")
        );
    }

    @Test
    void shouldThrowOnNull() {
        assertThrows(IllegalArgumentException.class, () -> GetMethodId.getMethodId(null));
    }
}
