
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.Slice;
import dev.quark.ton.core.dict.ParseDict;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ParseDictTest {

    private static Builder storeBits(Builder builder, String src) {
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '0') builder.storeBit(false);
            else if (c == '1') builder.storeBit(true);
            else throw new IllegalArgumentException("Invalid bit char: " + c);
        }
        return builder;
    }

    @Test
    public void shouldParseTheOneFromDocumentation() {
        Cell root =
                storeBits(Builder.beginCell(), "11001000")
                        .storeRef(
                                storeBits(Builder.beginCell(), "011000")
                                        .storeRef(storeBits(Builder.beginCell(), "1010011010000000010101001").endCell())
                                        .storeRef(storeBits(Builder.beginCell(), "1010000010000000100100001").endCell())
                                        .endCell()
                        )
                        .storeRef(storeBits(Builder.beginCell(), "1011111011111101111100100001").endCell())
                        .endCell();

        Map<BigInteger, Slice> loaded = ParseDict.parseDict(root.beginParse(), 16, s -> s);

        assertEquals(169L, loaded.get(BigInteger.valueOf(13)).loadUint(16));
        assertEquals(289L, loaded.get(BigInteger.valueOf(17)).loadUint(16));
        assertEquals(57121L, loaded.get(BigInteger.valueOf(239)).loadUint(16));
    }

    @Test
    public void shouldParseDictWithExoticsDoesNotThrow() {
        byte[] dict = Base64.getDecoder().decode(
                "te6cckECMQEABTYAAhOCCc7v0txVpwSwAQIDEwEE53fpbirTglgEAwIISAEBs+lknRDMs3k2joGjp+jknI61P2rMabC6L/qACC9w7jkAAQhIAQGcUdBjRLK2XTGk56evuoGpCTwBOhaNJ3gUFm8TAe0n5QAyAxMBAye9rIc4cIC4MAYFCEgBAW8xXyW0o5rBLIX+pOz+eoPl5Z0fBZeD+gw+8nlzCIBhAAADEwEBQI3GZN/+jNgvBwgDEwEATAHi3hq0fjgKCQgISAEB7X4mvTbvptXZtPaqq5gTrwdCqEJEl390/UB0ycmJCL4AAAhIAQH6TA1tA7w5MqlUZE/iIYZlmhFY/0nMfG9YEH4IA4oG9AAmAxEA/JVwvbaVd7guDAsISAEB16y7YCM4yG1hDzXPs2L9dvwYsYEkdrb8qZoGeOZl/PUAAAIRAOC+0jznnr0oLQ0CEQDgeIc2I3nB6A8OCEgBAe5bbwbC8lILAkcW7BvTGqfH7ackw/xrJ+4xJ9g0lay7ACICDwDcWf0tZ7wILBACDwDPXe4GVoRIEhEISAEBpqnx4FY+VMV5fZOCgk11aYemGilh+4jfDQXGfVnuO2QAHwIPAMwQLzuW7ygrEwIPAMGD4CnDLugVFAhIAQHhkmjGsVW1E//8jS7VtFUP/nG+13eBz2DH8b6lkRkfowATAg8AwL3BnpH5iCoWAg8AwFv9OrsVqBgXCEgBAUnJQzkhkkoQxP70VqQNlWC2ClDLq6thxGgnH+VDOUIdABICDwDAI9NSaezIKRkCDQC0EKoS2YgbGghIAQEUW2BGLrIxcR0xUglz9exM5sN90zhfxgdRBW0FkjOQBQAPAg0ApaMONrGIHRwISAEB20MCSueWmfetSar0Li+5Q8Ip7t01JoPqgAJhVAvv4PEADAINAKFiMLr7SCgeAg0AoV/mfAxIJx8CDQCgRMc88eghIAhIAQHFrdac2QaoB3A0l38UmVSRNUC4pYwh2FyGJ5Vl+MyhJgAJAgkAbRF4aCYiAgkAZzzR6CQjCEgBAax1zsp9u3C3amDOdSJD9mQdDCqhiDj+ZgliaFHwLgWgAAIBj7rR38jjeSour5CAiFzP5jBGI24SWg7B0O37+3W43agB1e+xhmG5Sli1Zv3ZU7LzkGnD7l80gqgY14NkUTcQu0YAAC5TRp8sgyUISAEBnuyDAqn2pYKXQ/wsg9nT0mXlzYlz/XD92d92SJgRWv8AAQhIAQHEi2bfoY75oVrUbsi8DucSPG1oWEcd3KHUGYp0M+RQMQACCEgBAWJ+cuHH5x28R42OXHVN6a1Jf7VXJHmTxS88Gp/rZs2wAAIISAEBOz2DLgJl9RiydhyAtaSVoJad3MYUNnANbILzFnfSgEkADQhIAQHMnAtnlPhbBcz2DH0IRfSKuD1YfeBVgFaujjkW+iHqDgAPCEgBAVyyY7T30fyOwTmieJ8TthLedj5loRH+lxKqP5GbivrBABcISAEBuusLuLgfE7diC4/aes/bPk0SUgkgruJUU+h2pBoayZwAFwhIAQFmUFOfgo2uUi6cXR4FvV6TYWbPc7i/d5MmEQOA4FGbgQAbCEgBAV7HcGhkdhUswzXpHOx7WG6dvrqJjJdLMlC+kTQaJBqhACMISAEBG14jhyXXJ7RLRZCnKyrvsoR9OsBOvnfdOdK6ADqrS88AIQhIAQFZ+0nqF8AuEf14qz7cSjcXQjasTFC5jvk1aLELPzNMHAAnCEgBAdzBkxtJradUwhDmKe2fCZCcreVUbqwio3hW3tN6N2dBAhTWF3cC"
        );

        Cell root = Cell.fromBoc(dict).get(0);
        Slice cs = root.beginParse();
        Slice dictSlice = cs.loadRef().beginParse();

        assertDoesNotThrow(() -> {
            Map<BigInteger, Slice> loaded = ParseDict.parseDict(dictSlice, 256, s -> s);
            assertNotNull(loaded);
        });
    }
}
