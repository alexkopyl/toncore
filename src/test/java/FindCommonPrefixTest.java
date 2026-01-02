import dev.quark.ton.core.dict.utils.FindCommonPrefix;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FindCommonPrefixTest {

    @Test
    public void shouldFindCommonPrefix() {
        assertEquals("0", FindCommonPrefix.findCommonPrefix(List.of(
                "0000111",
                "0101111",
                "0001111"
        )));
        assertEquals("000", FindCommonPrefix.findCommonPrefix(List.of(
                "0000111",
                "0001111",
                "0000101"
        )));
        assertEquals("", FindCommonPrefix.findCommonPrefix(List.of(
                "0000111",
                "1001111",
                "0000101"
        )));
    }
}
