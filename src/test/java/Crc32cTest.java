import dev.quark.ton.core.utils.Crc32c;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class Crc32cTest {

    @Test
    void shouldMatchTestVector() {
        byte[] data = "123456789".getBytes(StandardCharsets.UTF_8);
        byte[] crc = Crc32c.crc32c(data);

        // TS: Buffer.from('839206e3', 'hex') => bytes: 83 92 06 e3
        assertArrayEquals(new byte[] {
                (byte) 0x83, (byte) 0x92, (byte) 0x06, (byte) 0xE3
        }, crc);
    }
}
