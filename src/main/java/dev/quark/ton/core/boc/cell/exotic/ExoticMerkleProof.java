package dev.quark.ton.core.boc.cell.exotic;

import dev.quark.ton.core.boc.BitReader;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Builder;
import dev.quark.ton.core.boc.Cell;

import java.util.Arrays;
import java.util.List;

/**
 * Ported 1:1 from ton-core/src/boc/cell/exoticMerkleProof.ts
 */
public final class ExoticMerkleProof {

    public static final class Result {
        public final int proofDepth;
        public final byte[] proofHash;

        public Result(int proofDepth, byte[] proofHash) {
            this.proofDepth = proofDepth;
            this.proofHash = proofHash;
        }
    }

    private ExoticMerkleProof() {}

    public static Result parse(BitString bits, List<Cell> refs) {

        BitReader reader = new BitReader(bits);

        // type + hash + depth
        int size = 8 + 256 + 16;

        if (bits.length() != size) {
            throw new IllegalStateException(
                    "Merkle Proof cell must have exactly (8 + 256 + 16) bits, got \"" +
                            bits.length() + "\""
            );
        }

        if (refs.size() != 1) {
            throw new IllegalStateException(
                    "Merkle Proof cell must have exactly 1 ref, got \"" +
                            refs.size() + "\""
            );
        }

        // Check type
        long type = reader.loadUint(8);
        if (type != 3) {
            throw new IllegalStateException(
                    "Merkle Proof cell must have type 3, got \"" + type + "\""
            );
        }

        // Check data
        byte[] proofHash = reader.loadBuffer(32);
        int proofDepth = (int) reader.loadUint(16);

        byte[] refHash = refs.get(0).hash(0);
        int refDepth = refs.get(0).depth(0);

        if (proofDepth != refDepth) {
            throw new IllegalStateException(
                    "Merkle Proof cell ref depth must be exactly \"" +
                            proofDepth + "\", got \"" + refDepth + "\""
            );
        }

        if (!Arrays.equals(proofHash, refHash)) {
            throw new IllegalStateException(
                    "Merkle Proof cell ref hash must be exactly \"" +
                            bytesToHex(proofHash) + "\", got \"" +
                            bytesToHex(refHash) + "\""
            );
        }

        return new Result(proofDepth, proofHash);
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static Cell convertToMerkleProof(Cell c) {
        return Builder.beginCell()
                .storeUint(3, 8)
                .storeBuffer(c.hash(0))
                .storeUint(c.depth(0), 16)
                .storeRef(c)
                .endCellExotic();
    }

    private static Cell.Options exoticOptions() {
        Cell.Options o = new Cell.Options();
        o.exotic = true;
        return o;
    }
}
