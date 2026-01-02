import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.address.ExternalAddress;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.types.CommonMessageInfoTLB;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class CommonMessageInfoTLBTest {

    private static Address testAddress(int wc, String seed) {
        byte[] h = sha256(seed);
        return Address.parseRaw(wc + ":" + toHexLower(h));
    }

    private static ExternalAddress testExternalAddress(String seed) {
        // В ton-core testExternalAddress делает ExternalAddress с фиксированной битовой длиной.
        // Берём 256 бит как максимально “естественный” размер для вектора.
        byte[] h = sha256(seed);
        return new ExternalAddress(new BigInteger(1, h), 256);
    }

    private static byte[] sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHexLower(byte[] data) {
        char[] HEX = "0123456789abcdef".toCharArray();
        char[] out = new char[data.length * 2];
        int p = 0;
        for (byte b : data) {
            int v = b & 0xFF;
            out[p++] = HEX[v >>> 4];
            out[p++] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    @Test
    void shouldSerializeExternalInMessages_RoundtripLikeSpec() {
        CommonMessageInfoTLB.CommonMessageInfo msg = new CommonMessageInfoTLB.ExternalIn(
                testExternalAddress("addr-2"),
                testAddress(0, "addr-1"),
                BigInteger.ZERO
        );

        Cell cell = Builder.beginCell()
                .store(CommonMessageInfoTLB.storeCommonMessageInfo(msg))
                .endCell();

        CommonMessageInfoTLB.CommonMessageInfo msg2 =
                CommonMessageInfoTLB.loadCommonMessageInfo(cell.beginParse());

        Cell cell2 = Builder.beginCell()
                .store(CommonMessageInfoTLB.storeCommonMessageInfo(msg2))
                .endCell();

        assertTrue(cell.equals(cell2));
    }

    @Test
    void shouldSerializeExternalInMessages_WithNullMaybeExternalSrc() {
        // Доп. кейс: Maybe ExternalAddress = null
        CommonMessageInfoTLB.CommonMessageInfo msg = new CommonMessageInfoTLB.ExternalIn(
                null,
                testAddress(0, "addr-1"),
                BigInteger.ZERO
        );

        Cell cell = Builder.beginCell()
                .store(CommonMessageInfoTLB.storeCommonMessageInfo(msg))
                .endCell();

        CommonMessageInfoTLB.CommonMessageInfo msg2 =
                CommonMessageInfoTLB.loadCommonMessageInfo(cell.beginParse());

        assertTrue(msg2 instanceof CommonMessageInfoTLB.ExternalIn);
        CommonMessageInfoTLB.ExternalIn extIn = (CommonMessageInfoTLB.ExternalIn) msg2;
        assertNull(extIn.src());

        Cell cell2 = Builder.beginCell()
                .store(CommonMessageInfoTLB.storeCommonMessageInfo(msg2))
                .endCell();

        assertTrue(cell.equals(cell2));
    }
}
