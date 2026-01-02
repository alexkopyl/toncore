import dev.quark.ton.core.address.Address;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AddressTest {

    private static final String HASH =
            "2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3";

    @Test
    void shouldParseAddressesInVariousForms() {
        Address.ParseFriendlyResult address1 = Address.parseFriendly(
                "0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO"
        );
        Address.ParseFriendlyResult address2 = Address.parseFriendly(
                "kQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi47nL"
        );
        Address address3 = Address.parseRaw(
                "0:" + HASH
        );
        Address address4 = Address.parse(
                "-1:3333333333333333333333333333333333333333333333333333333333333333"
        );

        assertFalse(address1.isBounceable);
        assertTrue(address2.isBounceable);
        assertTrue(address1.isTestOnly);
        assertTrue(address2.isTestOnly);

        assertEquals(0, address1.address.workChain);
        assertEquals(0, address2.address.workChain);
        assertEquals(0, address3.workChain);

        assertArrayEquals(hexToBytes(HASH), address1.address.hash);
        assertArrayEquals(hexToBytes(HASH), address2.address.hash);
        assertArrayEquals(hexToBytes(HASH), address3.hash);

        assertEquals("0:" + HASH, address1.address.toRawString());
        assertEquals("0:" + HASH, address2.address.toRawString());
        assertEquals("0:" + HASH, address3.toRawString());

        assertEquals(-1, address4.workChain);
        assertArrayEquals(
                hexToBytes("3333333333333333333333333333333333333333333333333333333333333333"),
                address4.hash
        );
    }

    @Test
    void shouldSerializeToFriendlyForm() {
        Address address = Address.parseRaw("0:" + HASH);

        // Bounceable (defaults)
        assertEquals(
                "EQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4wJB",
                address.toString()
        );

        Address.ToStringArgs a1 = new Address.ToStringArgs();
        a1.testOnly = true;
        assertEquals(
                "kQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi47nL",
                address.toString(a1)
        );

        Address.ToStringArgs a2 = new Address.ToStringArgs();
        a2.urlSafe = false;
        assertEquals(
                "EQAs9VlT6S776tq3unJcP5Ogsj+ELLunLXuOb1EKcOQi4wJB",
                address.toString(a2)
        );

        Address.ToStringArgs a3 = new Address.ToStringArgs();
        a3.urlSafe = false;
        a3.testOnly = true;
        assertEquals(
                "kQAs9VlT6S776tq3unJcP5Ogsj+ELLunLXuOb1EKcOQi47nL",
                address.toString(a3)
        );

        // Non-Bounceable
        Address.ToStringArgs b1 = new Address.ToStringArgs();
        b1.bounceable = false;
        assertEquals(
                "UQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi41-E",
                address.toString(b1)
        );

        Address.ToStringArgs b2 = new Address.ToStringArgs();
        b2.bounceable = false;
        b2.testOnly = true;
        assertEquals(
                "0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO",
                address.toString(b2)
        );

        Address.ToStringArgs b3 = new Address.ToStringArgs();
        b3.bounceable = false;
        b3.urlSafe = false;
        assertEquals(
                "UQAs9VlT6S776tq3unJcP5Ogsj+ELLunLXuOb1EKcOQi41+E",
                address.toString(b3)
        );

        Address.ToStringArgs b4 = new Address.ToStringArgs();
        b4.bounceable = false;
        b4.urlSafe = false;
        b4.testOnly = true;
        assertEquals(
                "0QAs9VlT6S776tq3unJcP5Ogsj+ELLunLXuOb1EKcOQi4+QO",
                address.toString(b4)
        );
    }

    @Test
    void shouldImplementEquals() {
        Address address1 = Address.parseRaw("0:" + HASH);
        Address address2 = Address.parseRaw("0:" + HASH);
        Address address3 = Address.parseRaw("-1:" + HASH);
        Address address4 = Address.parseRaw("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e5");

        assertTrue(address1.equals(address2));
        assertTrue(address2.equals(address1));
        assertFalse(address2.equals(address4));
        assertFalse(address2.equals(address3));
        assertFalse(address4.equals(address3));
    }

    @Test
    void shouldThrowIfAddressIsInvalid() {
        // invalid raw hash length -> should be 31 bytes
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () ->
                Address.parseRaw("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422")
        );
        assertEquals("Invalid address hash length: 31", e1.getMessage());
        assertFalse(Address.isRaw("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422"));

        // odd-length hex in TS still results in 31 bytes -> same error expected
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
                Address.parseRaw("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e")
        );
        assertEquals("Invalid address hash length: 31", e2.getMessage());
        assertFalse(Address.isRaw("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e"));

        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () ->
                Address.parse("ton://EQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4wJB")
        );
        assertTrue(e3.getMessage().contains("Unknown address type"));

        IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class, () ->
                Address.parse("EQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4wJ")
        );
        assertTrue(e4.getMessage().contains("Unknown address type"));

        IllegalArgumentException e5 = assertThrows(IllegalArgumentException.class, () ->
                Address.parse("ton://transfer/EQDXDCFLXgiTrjGSNVBuvKPZVYlPn3J_u96xxLas3_yoRWRk")
        );
        assertTrue(e5.getMessage().contains("Unknown address type"));

        IllegalArgumentException e6 = assertThrows(IllegalArgumentException.class, () ->
                Address.parseFriendly("ton://transfer/EQDXDCFLXgiTrjGSNVBuvKPZVYlPn3J_u96xxLas3_yoRWRk")
        );
        assertEquals("Unknown address type", e6.getMessage());
        assertFalse(Address.isFriendly("ton://transfer/EQDXDCFLXgiTrjGSNVBuvKPZVYlPn3J_u96xxLas3_yoRWRk"));

        IllegalArgumentException e7 = assertThrows(IllegalArgumentException.class, () ->
                Address.parseFriendly("0:EQDXDCFLXgiTrjGSNVBuvKPZVYlPn3J_u96xxLas3_yoRWRk")
        );
        assertEquals("Unknown address type", e7.getMessage());
        assertFalse(Address.isFriendly("0:EQDXDCFLXgiTrjGSNVBuvKPZVYlPn3J_u96xxLas3_yoRWRk"));

        IllegalArgumentException e8 = assertThrows(IllegalArgumentException.class, () ->
                Address.parseFriendly("!@#$%^&*AAAAAAAAAAAAAA AAAAAAAAAA AAAAAAAAAAAA A")
        );
        assertEquals("Unknown address type", e8.getMessage());
        assertFalse(Address.isFriendly("!@#$%^&*AAAAAAAAAAAAAA AAAAAAAAAA AAAAAAAAAAAA A"));

        IllegalArgumentException e9 = assertThrows(IllegalArgumentException.class, () ->
                Address.parseFriendly("                                                ")
        );
        assertEquals("Unknown address type", e9.getMessage());
        assertFalse(Address.isFriendly("                                                "));
    }

    private static byte[] hexToBytes(String hex) {
        String h = hex.toLowerCase();
        if ((h.length() & 1) != 0) {
            throw new IllegalArgumentException("Odd hex length in test input");
        }
        byte[] out = new byte[h.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(h.charAt(i * 2), 16);
            int lo = Character.digit(h.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("Invalid hex in test input");
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
