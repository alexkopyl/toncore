package dev.quark.ton.core.boc.cell.wonder;

import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.CellType;
import dev.quark.ton.core.boc.cell.LevelMask;
import dev.quark.ton.core.boc.cell.descriptor.Descriptor;
import dev.quark.ton.core.boc.cell.exotic.ExoticPruned;
import dev.quark.ton.core.boc.cell.exotic.ExoticLibrary;
import dev.quark.ton.core.boc.cell.exotic.ExoticMerkleProof;
import dev.quark.ton.core.boc.cell.exotic.ExoticMerkleUpdate;
import dev.quark.ton.core.crypto.Sha256;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported 1:1 from ton-core/src/boc/cell/wonderCalculator.ts
 *
 * Replicates TON DataCell hash/depth logic.
 */
public final class WonderCalculator {

    private WonderCalculator() {}

    /* ============================================================ */
    /* =========================== Result ========================= */
    /* ============================================================ */

    public static final class Result {
        public final LevelMask mask;
        public final byte[][] hashes;
        public final int[] depths;

        public Result(LevelMask mask, byte[][] hashes, int[] depths) {
            this.mask = mask;
            this.hashes = hashes;
            this.depths = depths;
        }
    }

    /* ============================================================ */
    /* ======================= main logic ========================= */
    /* ============================================================ */

    public static Result calculate(CellType type, BitString bits, List<Cell> refs) {

        //
        // Resolve level mask
        //

        LevelMask levelMask;
        ExoticPruned pruned = null;

        if (type == CellType.Ordinary) {

            int mask = 0;
            for (Cell r : refs) {
                mask |= r.mask.value();
            }
            levelMask = new LevelMask(mask);

        } else if (type == CellType.PrunedBranch) {

            pruned = ExoticPruned.parse(bits, refs);
            levelMask = new LevelMask(pruned.mask);

        } else if (type == CellType.MerkleProof) {

            ExoticMerkleProof.parse(bits, refs);
            levelMask = new LevelMask(refs.get(0).mask.value() >> 1);

        } else if (type == CellType.MerkleUpdate) {

            ExoticMerkleUpdate.parse(bits, refs);
            levelMask = new LevelMask(
                    (refs.get(0).mask.value() | refs.get(1).mask.value()) >> 1
            );

        } else if (type == CellType.Library) {

            ExoticLibrary.parse(bits, refs);
            levelMask = new LevelMask();

        } else {
            throw new IllegalStateException("Unsupported exotic type");
        }

        //
        // Calculate hashes and depths
        //

        List<Integer> depths = new ArrayList<>();
        List<byte[]> hashes = new ArrayList<>();

        int hashCount = type == CellType.PrunedBranch ? 1 : levelMask.hashCount();
        int totalHashCount = levelMask.hashCount();
        int hashIOffset = totalHashCount - hashCount;

        for (int levelI = 0, hashI = 0; levelI <= levelMask.level(); levelI++) {

            if (!levelMask.isSignificant(levelI)) {
                continue;
            }

            if (hashI < hashIOffset) {
                hashI++;
                continue;
            }

            //
            // Bits
            //

            BitString currentBits;
            if (hashI == hashIOffset) {
                if (!(levelI == 0 || type == CellType.PrunedBranch)) {
                    throw new IllegalStateException("Invalid");
                }
                currentBits = bits;
            } else {
                if (!(levelI != 0 && type != CellType.PrunedBranch)) {
                    throw new IllegalStateException("Invalid: " + levelI + ", " + type);
                }
                currentBits = new BitString(hashes.get(hashI - hashIOffset - 1), 0, 256);
            }

            //
            // Depth
            //

            int currentDepth = 0;
            for (Cell c : refs) {
                int childDepth;
                if (type == CellType.MerkleProof || type == CellType.MerkleUpdate) {
                    childDepth = c.depth(levelI + 1);
                } else {
                    childDepth = c.depth(levelI);
                }
                currentDepth = Math.max(currentDepth, childDepth);
            }
            if (!refs.isEmpty()) {
                currentDepth++;
            }

            //
            // Hash
            //

            int appliedMask = levelMask.apply(levelI).value();
            byte[] repr = Descriptor.getRepr(bits, currentBits, refs, levelI, appliedMask, type);
            byte[] hash = Sha256.sha256Sync(repr);

            //
            // Persist
            //

            int destI = hashI - hashIOffset;
            if (depths.size() <= destI) {
                depths.add(currentDepth);
                hashes.add(hash);
            } else {
                depths.set(destI, currentDepth);
                hashes.set(destI, hash);
            }

            hashI++;
        }

        //
        // Resolve all levels
        //

        byte[][] resolvedHashes = new byte[4][];
        int[] resolvedDepths = new int[4];

        if (pruned != null) {
            for (int i = 0; i < 4; i++) {
                int hashIndex = levelMask.apply(i).hashIndex();
                int thisHashIndex = levelMask.hashIndex();
                if (hashIndex != thisHashIndex) {
                    resolvedHashes[i] = pruned.pruned[hashIndex].hash;
                    resolvedDepths[i] = pruned.pruned[hashIndex].depth;
                } else {
                    resolvedHashes[i] = hashes.get(0);
                    resolvedDepths[i] = depths.get(0);
                }
            }
        } else {
            for (int i = 0; i < 4; i++) {
                int idx = levelMask.apply(i).hashIndex();
                resolvedHashes[i] = hashes.get(idx);
                resolvedDepths[i] = depths.get(idx);
            }
        }

        //
        // Result
        //

        return new Result(levelMask, resolvedHashes, resolvedDepths);
    }
}
