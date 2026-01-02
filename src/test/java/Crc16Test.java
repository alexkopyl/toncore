import dev.quark.ton.core.utils.Crc16;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class Crc16Test {

    @Test
    void shouldMatchTestVector() {
        byte[] data = "123456789".getBytes(StandardCharsets.UTF_8);
        byte[] crc = Crc16.crc16(data);

        assertArrayEquals(new byte[] { (byte) 0x31, (byte) 0xC3 }, crc);
    }
}
