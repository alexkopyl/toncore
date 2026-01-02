import dev.quark.ton.core.utils.Convert;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConvertTest {

    private record StringCase(String nano, String real) {}
    private record NumberCase(String nano, double real) {}

    private static final List<StringCase> STRING_CASES = List.of(
            new StringCase("1000000000", "1"),
            new StringCase("10000000000", "10"),
            new StringCase("100000000", "0.1"),
            new StringCase("330000000", "0.33"),
            new StringCase("1", "0.000000001"),
            new StringCase("10000000001", "10.000000001"),
            new StringCase("1000000000000001", "1000000.000000001"),
            new StringCase("100000000000000000000", "100000000000")
    );

    private static final List<NumberCase> NUMBER_CASES = List.of(
            new NumberCase("0", -0.0),
            new NumberCase("0", 0.0),
            new NumberCase("10000000000000000000000000000000000000000000000000000000000000000000000000", 1e64),
            new NumberCase("1000000000", 1.0),
            new NumberCase("10000000000", 10.0),
            new NumberCase("100000000", 0.1),
            new NumberCase("330000000", 0.33),
            new NumberCase("1", 0.000000001),
            new NumberCase("10000000001", 10.000000001),
            new NumberCase("1000000000000001", 1000000.000000001),
            new NumberCase("100000000000000000000", 100000000000.0)
    );

    @Test
    public void shouldThrowForNaN() {
        assertThrows(IllegalArgumentException.class, () -> Convert.toNano(Double.NaN));
    }

    @Test
    public void shouldThrowForInfinity() {
        assertThrows(IllegalArgumentException.class, () -> Convert.toNano(Double.POSITIVE_INFINITY));
    }

    @Test
    public void shouldThrowForMinusInfinity() {
        assertThrows(IllegalArgumentException.class, () -> Convert.toNano(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void shouldThrowDueToInsufficientPrecisionOfNumber() {
        // TS: toNano(10000000.000000001) should throw
        assertThrows(IllegalArgumentException.class, () -> Convert.toNano(10000000.000000001));
    }

    @Test
    public void shouldConvertNumbersToNano() {
        for (NumberCase c : NUMBER_CASES) {
            BigInteger got = Convert.toNano(c.real());
            assertEquals(new BigInteger(c.nano()), got, "real=" + c.real());
        }
    }

    @Test
    public void shouldConvertStringsToNano() {
        for (StringCase c : STRING_CASES) {
            BigInteger got = Convert.toNano(c.real());
            assertEquals(new BigInteger(c.nano()), got, "real=" + c.real());
        }
    }

    @Test
    public void shouldConvertFromNano() {
        for (StringCase c : STRING_CASES) {
            String got = Convert.fromNano(c.nano());
            assertEquals(c.real(), got, "nano=" + c.nano());
        }
    }
}
