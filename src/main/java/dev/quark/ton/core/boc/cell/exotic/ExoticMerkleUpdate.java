package dev.quark.ton.core.boc.cell.exotic;

import dev.quark.ton.core.boc.BitReader;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Cell;

import java.util.Arrays;
import java.util.List;

/**
 * Ported 1:1 from ton-core/src/boc/cell/exoticMerkleUpdate.ts
 */
public final class ExoticMerkleUpdate {

    public static final class Result {
        public final int proofDepth1;
        public final int proofDepth2;
        public final byte[] proofHash1;
        public final byte[] proofHash2;

        public Result(int proofDepth1, int proofDepth2, byte[] proofHash1, byte[] proofHash2) {
            this.proofDepth1 = proofDepth1;
            this.proofDepth2 = proofDepth2;
            this.proofHash1 = proofHash1;
            this.proofHash2 = proofHash2;
        }
    }

    private ExoticMerkleUpdate() {}

    public static Result parse(BitString bits, List<Cell> refs) {

        BitReader reader = new BitReader(bits);

        // type + hash + hash + depth + depth
        int size = 8 + (2 * (256 + 16));

        if (bits.length() != size) {
            throw new IllegalStateException(
                    "Merkle Update cell must have exactly (8 + (2 * (256 + 16))) bits, got \"" +
                            bits.length() + "\""
            );
        }

        if (refs.size() != 2) {
            throw new IllegalStateException(
                    "Merkle Update cell must have exactly 2 refs, got \"" +
                            refs.size() + "\""
            );
        }

        long type = reader.loadUint(8);
        if (type != 4) {
            throw new IllegalStateException(
                    "Merkle Update cell type must be exactly 4, got \"" + type + "\""
            );
        }

        byte[] proofHash1 = reader.loadBuffer(32);
        byte[] proofHash2 = reader.loadBuffer(32);
        int proofDepth1 = (int) reader.loadUint(16);
        int proofDepth2 = (int) reader.loadUint(16);

        if (proofDepth1 != refs.get(0).depth(0)) {
            throw new IllegalStateException(
                    "Merkle Update cell ref depth must be exactly \"" +
                            proofDepth1 + "\", got \"" + refs.get(0).depth(0) + "\""
            );
        }

        if (!Arrays.equals(proofHash1, refs.get(0).hash(0))) {
            throw new IllegalStateException(
                    "Merkle Update cell ref hash must be exactly \"" +
                            bytesToHex(proofHash1) + "\", got \"" +
                            bytesToHex(refs.get(0).hash(0)) + "\""
            );
        }

        if (proofDepth2 != refs.get(1).depth(0)) {
            throw new IllegalStateException(
                    "Merkle Update cell ref depth must be exactly \"" +
                            proofDepth2 + "\", got \"" + refs.get(1).depth(0) + "\""
            );
        }

        if (!Arrays.equals(proofHash2, refs.get(1).hash(0))) {
            throw new IllegalStateException(
                    "Merkle Update cell ref hash must be exactly \"" +
                            bytesToHex(proofHash2) + "\", got \"" +
                            bytesToHex(refs.get(1).hash(0)) + "\""
            );
        }

        return new Result(proofDepth1, proofDepth2, proofHash1, proofHash2);
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
