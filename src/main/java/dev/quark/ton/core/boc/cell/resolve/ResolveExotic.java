package dev.quark.ton.core.boc.cell.resolve;

import dev.quark.ton.core.boc.BitReader;
import dev.quark.ton.core.boc.BitString;
import dev.quark.ton.core.boc.Cell;
import dev.quark.ton.core.boc.CellType;
import dev.quark.ton.core.boc.cell.LevelMask;
import dev.quark.ton.core.boc.cell.exotic.ExoticLibrary;
import dev.quark.ton.core.boc.cell.exotic.ExoticMerkleProof;
import dev.quark.ton.core.boc.cell.exotic.ExoticMerkleUpdate;
import dev.quark.ton.core.boc.cell.exotic.ExoticPruned;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported 1:1 from ton-core/src/boc/cell/resolveExotic.ts
 */
public final class ResolveExotic {

    private ResolveExotic() {}

    /* ============================================================ */
    /* =========================== Result ========================= */
    /* ============================================================ */

    public static final class Result {
        public final CellType type;
        public final int[] depths;
        public final byte[][] hashes;
        public final LevelMask mask;

        public Result(CellType type, int[] depths, byte[][] hashes, LevelMask mask) {
            this.type = type;
            this.depths = depths;
            this.hashes = hashes;
            this.mask = mask;
        }
    }

    /* ============================================================ */
    /* ====================== resolvers =========================== */
    /* ============================================================ */

    private static Result resolvePruned(BitString bits, List<Cell> refs) {

        // Parse pruned cell
        ExoticPruned pruned = ExoticPruned.parse(bits, refs);

        // Calculate parameters
        int[] depths = new int[pruned.pruned.length];
        byte[][] hashes = new byte[pruned.pruned.length][];
        LevelMask mask = new LevelMask(pruned.mask);

        for (int i = 0; i < pruned.pruned.length; i++) {
            depths[i] = pruned.pruned[i].depth;
            hashes[i] = pruned.pruned[i].hash;
        }

        return new Result(CellType.PrunedBranch, depths, hashes, mask);
    }

    private static Result resolveLibrary(BitString bits, List<Cell> refs) {

        // Parse library cell
        ExoticLibrary.parse(bits, refs);

        // Calculate parameters
        int[] depths = new int[0];
        byte[][] hashes = new byte[0][];
        LevelMask mask = new LevelMask();

        return new Result(CellType.Library, depths, hashes, mask);
    }

    private static Result resolveMerkleProof(BitString bits, List<Cell> refs) {

        // Parse merkle proof cell
        ExoticMerkleProof.parse(bits, refs);

        // Calculate parameters
        int[] depths = new int[0];
        byte[][] hashes = new byte[0][];
        LevelMask mask = new LevelMask(refs.get(0).level() >> 1);

        return new Result(CellType.MerkleProof, depths, hashes, mask);
    }

    private static Result resolveMerkleUpdate(BitString bits, List<Cell> refs) {

        // Parse merkle update cell
        ExoticMerkleUpdate.parse(bits, refs);

        // Calculate parameters
        int[] depths = new int[0];
        byte[][] hashes = new byte[0][];
        LevelMask mask = new LevelMask((refs.get(0).level() | refs.get(1).level()) >> 1);

        return new Result(CellType.MerkleUpdate, depths, hashes, mask);
    }

    /* ============================================================ */
    /* ======================= dispatcher ========================= */
    /* ============================================================ */

    public static Result resolve(BitString bits, List<Cell> refs) {
        BitReader reader = new BitReader(bits);
        long type = reader.preloadUint(8);

        if (type == 1) {
            return resolvePruned(bits, refs);
        }

        if (type == 2) {
            return resolveLibrary(bits, refs);
        }

        if (type == 3) {
            return resolveMerkleProof(bits, refs);
        }

        if (type == 4) {
            return resolveMerkleUpdate(bits, refs);
        }

        throw new IllegalArgumentException("Invalid exotic cell type: " + type);
    }
}
