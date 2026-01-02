package dev.quark.ton.core.boc.cell.exotic;

import dev.quark.ton.core.boc.BitReader;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.cell.LevelMask;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported 1:1 from ton-core/src/boc/cell/exoticPruned.ts
 */
public final class ExoticPruned {

    public static final class Pruned {
        public final int depth;
        public final byte[] hash;

        public Pruned(int depth, byte[] hash) {
            this.depth = depth;
            this.hash = hash;
        }
    }

    public final int mask;
    public final Pruned[] pruned;

    private ExoticPruned(int mask, Pruned[] pruned) {
        this.mask = mask;
        this.pruned = pruned;
    }

    public static ExoticPruned parse(BitString bits, List<Cell> refs) {

        BitReader reader = new BitReader(bits);

        // Check type
        long type = reader.loadUint(8);
        if (type != 1) {
            throw new IllegalStateException(
                    "Pruned branch cell must have type 1, got \"" + type + "\""
            );
        }

        // Check refs
        if (!refs.isEmpty()) {
            throw new IllegalStateException(
                    "Pruned Branch cell can't has refs, got \"" + refs.size() + "\""
            );
        }

        // Resolve mask
        LevelMask mask;
        if (bits.length() == 280) {

            // Special case for config proof
            mask = new LevelMask(1);

        } else {

            mask = new LevelMask((int) reader.loadUint(8));
            if (mask.level() < 1 || mask.level() > 3) {
                throw new IllegalStateException(
                        "Pruned Branch cell level must be >= 1 and <= 3, got \"" +
                                mask.level() + "/" + mask.value() + "\""
                );
            }

            int size =
                    8 + 8 +
                            (mask.apply(mask.level() - 1).hashCount() * (256 + 16));

            if (bits.length() != size) {
                throw new IllegalStateException(
                        "Pruned branch cell must have exactly " + size +
                                " bits, got \"" + bits.length() + "\""
                );
            }
        }

        // Read hashes
        List<byte[]> hashes = new ArrayList<>();
        for (int i = 0; i < mask.level(); i++) {
            hashes.add(reader.loadBuffer(32));
        }

        // Read depths
        List<Integer> depths = new ArrayList<>();
        for (int i = 0; i < mask.level(); i++) {
            depths.add((int) reader.loadUint(16));
        }

        // Combine
        Pruned[] pruned = new Pruned[mask.level()];
        for (int i = 0; i < mask.level(); i++) {
            pruned[i] = new Pruned(depths.get(i), hashes.get(i));
        }

        return new ExoticPruned(mask.value(), pruned);
    }
}
