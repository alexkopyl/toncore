package dev.quark.ton.core.boc.cell.exotic;

import dev.quark.ton.core.boc.BitReader;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Cell;

import java.util.List;

/**
 * Ported 1:1 from ton-core/src/boc/cell/exoticLibrary.ts
 */
public final class ExoticLibrary {

    private ExoticLibrary() {}

    public static void parse(BitString bits, List<Cell> refs) {

        BitReader reader = new BitReader(bits);

        // type + hash
        int size = 8 + 256;

        if (bits.length() != size) {
            throw new IllegalStateException(
                    "Library cell must have exactly (8 + 256) bits, got \"" +
                            bits.length() + "\""
            );
        }

        // Check type
        long type = reader.loadUint(8);
        if (type != 2) {
            throw new IllegalStateException(
                    "Library cell must have type 2, got \"" + type + "\""
            );
        }
    }
}
