package dev.quark.ton.core.boc.cell.descriptor;

import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.CellType;
import dev.quark.ton.core.boc.utils.PaddedBits;

import java.util.List;

/**
 * Ported 1:1 from ton-core/src/boc/cell/descriptor.ts
 */
public final class Descriptor {

    private Descriptor() {}

    /**
     * TS:
     * return refs.length + (type !== CellType.Ordinary ? 1 : 0) * 8 + levelMask * 32;
     */
    public static int getRefsDescriptor(List<Cell> refs, int levelMask, CellType type) {
        return refs.size() + (type != CellType.Ordinary ? 1 : 0) * 8 + levelMask * 32;
    }

    /**
     * TS:
     * let len = bits.length;
     * return Math.ceil(len / 8) + Math.floor(len / 8);
     */
    public static int getBitsDescriptor(BitString bits) {
        int len = bits.length();
        return (int) Math.ceil(len / 8.0) + (len / 8);
    }

    /**
     * TS:
     * getRepr(originalBits, bits, refs, level, levelMask, type)
     */
    public static byte[] getRepr(
            BitString originalBits,
            BitString bits,
            List<Cell> refs,
            int level,
            int levelMask,
            CellType type
    ) {
        // Allocate
        int bitsLen = (int) Math.ceil(bits.length() / 8.0);
        byte[] repr = new byte[2 + bitsLen + (2 + 32) * refs.size()];

        int reprCursor = 0;

        // Write descriptors
        repr[reprCursor++] = (byte) getRefsDescriptor(refs, levelMask, type);
        repr[reprCursor++] = (byte) getBitsDescriptor(originalBits);

        // Write bits (padded)
        byte[] padded = PaddedBits.bitsToPaddedBuffer(bits);
        System.arraycopy(padded, 0, repr, reprCursor, bitsLen);
        reprCursor += bitsLen;

        // Write refs depths
        for (Cell c : refs) {
            int childDepth;
            if (type == CellType.MerkleProof || type == CellType.MerkleUpdate) {
                childDepth = c.depth(level + 1);
            } else {
                childDepth = c.depth(level);
            }
            repr[reprCursor++] = (byte) (childDepth / 256);
            repr[reprCursor++] = (byte) (childDepth % 256);
        }

        // Write refs hashes
        for (Cell c : refs) {
            byte[] childHash;
            if (type == CellType.MerkleProof || type == CellType.MerkleUpdate) {
                childHash = c.hash(level + 1);
            } else {
                childHash = c.hash(level);
            }
            System.arraycopy(childHash, 0, repr, reprCursor, 32);
            reprCursor += 32;
        }

        return repr;
    }
}
