import dev.quark.ton.core.utils.BitsForNumber;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class BitsForNumberTest {

    @Test
    void shouldWork() {
        assertEquals(1, BitsForNumber.bitsForNumber(0, BitsForNumber.Mode.INT));
        assertEquals(2, BitsForNumber.bitsForNumber(1, BitsForNumber.Mode.INT));
        assertEquals(1, BitsForNumber.bitsForNumber(-1, BitsForNumber.Mode.INT));
        assertEquals(3, BitsForNumber.bitsForNumber(-2, BitsForNumber.Mode.INT));

        // (не обязательно по TS-спеке, но полезно проверить BigInteger overload не ломает поведение)
        assertEquals(1, BitsForNumber.bitsForNumber(BigInteger.ZERO, BitsForNumber.Mode.INT));
        assertEquals(1, BitsForNumber.bitsForNumber(BigInteger.valueOf(-1), BitsForNumber.Mode.INT));
    }
}
