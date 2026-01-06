package dev.quark.ton.core.utils;

import dev.quark.ton.core.address.Address;
import dev.quark.ton.core.address.ExternalAddress;
import dev.quark.ton.core.test.Prando;

import java.math.BigInteger;

public final class TestAddress {

    private TestAddress() {}

    public static Address testAddress(int workchain, String seed) {
        Prando random = new Prando(seed);
        byte[] hash = new byte[32];
        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte) random.nextInt(0, 255);
        }
        return new Address(workchain, hash);
    }

    public static ExternalAddress testExternalAddress(String seed) {
        Prando random = new Prando(seed);
        byte[] hash = new byte[32];
        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte) random.nextInt(0, 255);
        }

        // TS: BigInt('0x' + hash.toString('hex')) -> unsigned integer
        BigInteger v = new BigInteger(1, hash);

        // TS: bitsForNumber(v, 'uint')
        int bits = BitsForNumber.bitsForNumber(v, BitsForNumber.Mode.UINT);

        return new ExternalAddress(v, bits);
    }
}
