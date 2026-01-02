import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;

import dev.quark.ton.core.types.Transaction;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {

    private static Cell cellFromBocBase64(String bocBase64) {
        String normalized = bocBase64.replaceAll("\\s+", "");
        byte[] boc = Base64.getDecoder().decode(normalized);
        Cell[] roots = Cell.fromBoc(boc).toArray(new Cell[0]);
        assertNotNull(roots);
        assertTrue(roots.length > 0, "BOC must have at least one root cell");
        return roots[0];
    }

    private static Cell roundTrip(Transaction tx) {
        Builder b = Builder.beginCell();
        tx.store(b);
        return b.endCell();
    }

    @Test
    void shouldParseTransaction() {
        // Source: explorer.toncoin.org (см. TS spec)
        String boc = "te6cckECCgEAAlMAA7V4Pf1VLmNym0cvy8yMRevMZpFwJVi2jsdSfhukA6DzGoAAAU1Ed9DUFmPJqTvOirSq2SKzYQ5GyQgvd9+38bqLQeGJ8YYrkwEwAAFNRGeacBYYzLegADRpVFhoAQIDAgHgBAUAgnKs+GZiNansGUYB+rKGLa25KuWgzm0WaeC5p+NLonoeFBg7+If0w+KZtCRH5Mx+9HCC8Pihk1IvrTPyRowEaTRLAg8MQMYZbXqEQAgJAd+IAQe/qqXMblNo5fl5kYi9eYzSLgSrFtHY6k/DdIB0HmNQB3H/g30bYqz72JAcnKJjRhgkmca92JLgIBGap3csfDt4Jk4S1186lCTQKuGZLHb97aw106oJRO8jslWF11AnUCFNTRi7DGZdIAAAAIAcBgEB3wcAdEIACasmTDZcDR+1HcxuZZBHXmDDvLMcX2Eijok8WCLyCyEwSMJzlQAAAAAAAAAAAAAAAAAAAAAAAAAAvUgBB7+qpcxuU2jl+XmRiL15jNIuBKsW0djqT8N0gHQeY1EABNWTJhsuBo/ajuY3MsgjrzBh3lmOL7CRR0SeLBF5BZCYJGE5yoAABhRYYAAAKaiO+hqEwxmW9AAAAABAAJ1BdkMTiAAAAAAAAAAAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAG/Jh6EgTBRYQAAAAAAAAgAAAAAAA4SB9Dp1g8lBEAkVf+gygVyC7sUl7wdSG9SEX3iBd2MqQFAXjC98i7E=";

        Cell cell = cellFromBocBase64(boc);
        Transaction tx = Transaction.load(cell.beginParse());

        Cell cell2 = roundTrip(tx);

        assertTrue(cell.equals(cell2), "Transaction must round-trip to identical cell");

    }

    @Test
    void shouldParseTransactionAndHash() {
        String boc = "te6cckECCgEAAlMAA7V4Pf1VLmNym0cvy8yMRevMZpFwJVi2jsdSfhukA6DzGoAAAU1Ed9DUFmPJqTvOirSq2SKzYQ5GyQgvd9+38bqLQeGJ8YYrkwEwAAFNRGeacBYYzLegADRpVFhoAQIDAgHgBAUAgnKs+GZiNansGUYB+rKGLa25KuWgzm0WaeC5p+NLonoeFBg7+If0w+KZtCRH5Mx+9HCC8Pihk1IvrTPyRowEaTRLAg8MQMYZbXqEQAgJAd+IAQe/qqXMblNo5fl5kYi9eYzSLgSrFtHY6k/DdIB0HmNQB3H/g30bYqz72JAcnKJjRhgkmca92JLgIBGap3csfDt4Jk4S1186lCTQKuGZLHb97aw106oJRO8jslWF11AnUCFNTRi7DGZdIAAAAIAcBgEB3wcAdEIACasmTDZcDR+1HcxuZZBHXmDDvLMcX2Eijok8WCLyCyEwSMJzlQAAAAAAAAAAAAAAAAAAAAAAAAAAvUgBB7+qpcxuU2jl+XmRiL15jNIuBKsW0djqT8N0gHQeY1EABNWTJhsuBo/ajuY3MsgjrzBh3lmOL7CRR0SeLBF5BZCYJGE5yoAABhRYYAAAKaiO+hqEwxmW9AAAAABAAJ1BdkMTiAAAAAAAAAAAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAG/Jh6EgTBRYQAAAAAAAAgAAAAAAA4SB9Dp1g8lBEAkVf+gygVyC7sUl7wdSG9SEX3iBd2MqQFAXjC98i7E=";

        Cell cell = cellFromBocBase64(boc);
        Transaction tx = Transaction.load(cell.beginParse());

        assertArrayEquals(cell.hash(), tx.hash(), "tx.hash() must equal original cell.hash()");
        assertTrue(tx.raw().equals(cell), "tx.raw must equal original root cell");
    }

    @Test
    void shouldParseTickTockTransactions() {
        String boc = "te6cckECBgEAATIAA69zRRfHvfUYfFWvT4th/cMhWIx6t2je4ksAbfKRBkWNfPAAAU76vLzwNWZ7xZALK0LgKzOhLytPsuTA0xeefgUOOoutURmfnnowAAFO+ry88BYZJ8ZwABQIAQIDAAEgAIJylqTCiKw1AYPxh8CsC3VDbY7yFlU0TmTCts3A0+5em+JY4alwBNV+0DtCbXo8QiEQ9DmV3wfpinUd6ThveMbXjwIFMDA0BAUAnkJmTmJaAAAAAAAAAAAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAaWAAAACWAAAABAAGAAAAAAAFGa6E8XuPiyICapdf9V8asZ/eSnaHRNIXjfpju1M+EHpAkCa8Fcsu4Q==";

        Cell cell = cellFromBocBase64(boc);
        Transaction tx = Transaction.load(cell.beginParse());

        Cell cell2 = roundTrip(tx);
        assertTrue(cell.equals(cell2), "Tick/Tock tx must round-trip");
    }

    @Test
    void shouldParseStateInitTransaction() {
        String boc = """
                te6cckECDAEAAu4AA7V898cPGd+25qY9UQrwuX9qrBpR1LR8TLjI6rQdNfeUEuAAAVKVz+HYEWZHs5hb3tPdVm6TyS0dYFMNN3PGKg8TV6Jgm5arcIAAAAFSdO3MjDYZ6HTQACRr/CkIAQIDAgHgBAUAgnLohCe0UvXyi7CExAkb9KB7KAKBd6TEhWHteK/pi8qfDA5KuYdIiud3mWwWCASe/Ka43A8FgsOgF8hL4KarrIkkAhEMgEIGGW16hEAKCwPhiAGe+OHjO/bc1MeqIV4XL+1Vg0o6lo+JlxkdVoOmvvKCXBGNQVPKLsIFvTtEsIAl4RIi2DTiB0eU25iJf1147ysJs7Vy6LRM2H5IYI8F+UOC6bDfjx64ZwQoFP2bvzSbTIphhTU0Yuwz0O9AAAAAAHAGBwgBAd8JAN7/ACDdIIIBTJe6IYIBM5y6sZ9xsO1E0NMf0x8x1wv/4wTgpPJggwjXGCDTH9Mf0x/4IxO78mPtRNDTH9Mf0//RUTK68qFRRLryogT5AVQQVfkQ8qP4AJMg10qW0wfUAvsA6NEBpMjLH8sfy//J7VQAUAAAAAApqaMXiOOuWdrrclMfTVlh2SoiW1/FeDcg77Wae9OQL/uLWpEAcGIAY5WvyYiEO64KphFdElBfUhViX8zkDYB0vInaJ+cCklIgF9eEAAAAAAAAAAAAAAAAAAAAAAAAALloAZ744eM79tzUx6ohXhcv7VWDSjqWj4mXGR1Wg6a+8oJdADHK1+TEQh3XBVMIrokoL6kKsS/mcgbAOl5E7RPzgUkpEAvrwgAGFFhgAAAqUrn8OwTDPQ6aAAAAAEAAnUF2QxOIAAAAAAAAAAAQgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAb8mHoSBMFFhAAAAAAAACAAAAAAADAjkoeILAGVO2lRti7r53KaoKzdokjAnKg8yAJDgjQS5AUBcM2+JyJQ==
                """;

        Cell cell = cellFromBocBase64(boc);
        Transaction tx = Transaction.load(cell.beginParse());

        Cell cell2 = roundTrip(tx);
        assertTrue(cell.equals(cell2), "StateInit tx must round-trip");
    }

    @Test
    void shouldParseMessagesWithExternalAddresses() {
        String boc = "te6cckECCgEAAkUAA7d6SR1j8Hun7vtMufaFSEzpCJ1auuyXwVhYIi8EylkqmsAAAU2eO41UEeRJr6vFqCcKIH/At25aJERfuKnnFNvEUGZfZXngcMFgAAFNniDZZBYY3z6QADSAmSAlCAUEAQITDJIthiAa0nSEQAMCAG/JzEtATMtyiAAAAAAAAgAAAAAAA5JB3YvT7VrkPXxN485b+s1ZzT6izdF5jCfNCmC9DSz0QFAWTACdQr8jE4gAAAAAAAAAACDAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIACCciw0bvSHQSzpSCq6INjDyTZ0z/6sSWYTMKFYrSQ9Wna3V4riNEWKRC5izQTxkFRpyfO/HYX80uSsctpuApvP8ucCAeAIBgEB3wcAs0n/SSOsfg90/d9plz7QqQmdIROrV12S+CsLBEXgmUslU1kAMKD3eVRXGaUISSzmQvyNMVyqDyUSV8CLx1J57yPaMUMUXSHboAAGy3O8AAAps8dxqoTDG+fSQAFKkFAbP+kkdY/B7p+77TLn2hUhM6QidWrrsl8FYWCIvBMpZKprAQkA8k1pbmUAYY33HsKD3eVRXGaUISSzmQvyNMVyqDyUSV8CLx1J57yPaMUMXlJVe7JQw1jSHC/5YUf/q2idXQh6cYVEjRYxL1YGO0JsibseRbOvoYvImYC6fmv2XlJVe7JQw1jSHC/5YUf/q2idXQh6cYVEjRYxL1YGO0KwLxvj";

        Cell cell = cellFromBocBase64(boc);
        Transaction tx = Transaction.load(cell.beginParse());

        Cell cell2 = roundTrip(tx);
        assertTrue(cell.equals(cell2), "External-address messages tx must round-trip");
    }



    static String diffCellHashDeep(Cell a, Cell b, String path) {
        if (a == b) return null;
        if (a == null || b == null) return path + ": one is null";

        // quick check: if hashes equal -> assume equal
        if (java.util.Arrays.equals(a.hash(), b.hash())) {
            return null;
        }

        // If bits/refs shape differs, report immediately
        if (a.bits.length() != b.bits.length()) {
            return path + ": bits length differ: " + a.bits.length() + " vs " + b.bits.length();
        }
        if (!a.bits.equalsBits(b.bits)) {
            return path + ": bits differ: " + a.bits + " vs " + b.bits;
        }
        if (a.refs.size() != b.refs.size()) {
            return path + ": refs count differ: " + a.refs.size() + " vs " + b.refs.size();
        }

        // Dive into children first to find the deepest mismatch
        for (int i = 0; i < a.refs.size(); i++) {
            String r = diffCellHashDeep(a.refs.get(i), b.refs.get(i), path + ".refs[" + i + "]");
            if (r != null) return r;
        }

        // Children match by hash, bits+refs match, but this node hash differs => metadata issue (exotic/level/etc)
        return path + ": hash differs, but bits+refs and children match => metadata mismatch. "
                + meta(a) + " vs " + meta(b)
                + " | hash: " + hex(a.hash()) + " vs " + hex(b.hash());
    }

    static String meta(Cell c) {
        return "{exotic=" + getBoolReflect(c, "exotic", "isExotic")
                + ", levelMask=" + getIntReflect(c, "levelMask", "getLevelMask", "levelMask()")
                + ", bits=" + c.bits.length()
                + ", refs=" + c.refs.size()
                + "}";
    }

    static Boolean getBoolReflect(Object o, String fieldName, String methodName) {
        try {
            var f = o.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(o);
            return (v instanceof Boolean) ? (Boolean) v : null;
        } catch (Exception ignored) { }
        try {
            var m = o.getClass().getMethod(methodName);
            Object v = m.invoke(o);
            return (v instanceof Boolean) ? (Boolean) v : null;
        } catch (Exception ignored) { }
        return null;
    }

    static Integer getIntReflect(Object o, String fieldName, String method1, String method2) {
        try {
            var f = o.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(o);
            return (v instanceof Number) ? ((Number) v).intValue() : null;
        } catch (Exception ignored) { }
        for (String mn : new String[]{method1, method2}) {
            try {
                var m = o.getClass().getMethod(mn);
                Object v = m.invoke(o);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (Exception ignored) { }
        }
        return null;
    }

    static String hex(byte[] x) {
        final char[] HEX = "0123456789abcdef".toCharArray();
        char[] out = new char[x.length * 2];
        for (int i = 0; i < x.length; i++) {
            int v = x[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

}
