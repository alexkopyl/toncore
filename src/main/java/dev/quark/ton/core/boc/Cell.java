package dev.quark.ton.core.boc;

import dev.quark.ton.core.boc.cell.LevelMask;
import dev.quark.ton.core.boc.cell.serialization.BocSerialization;
import dev.quark.ton.core.boc.cell.resolve.ResolveExotic;
import dev.quark.ton.core.boc.cell.wonder.WonderCalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Cell as described in TVM spec
 *
 * Ported 1:1 from ton-core/src/boc/Cell.ts
 */
public final class Cell {

    public static final Cell EMPTY = new Cell();

    /* ============================================================ */
    /* ======================= static helpers ===================== */
    /* ============================================================ */

    /**
     * Deserialize cells from BOC
     */
    public static List<Cell> fromBoc(byte[] src) {
        return BocSerialization.deserializeBoc(src);
    }

    /**
     * Deserialize a single cell from base64 BOC
     */
    public static Cell fromBase64(String src) {
        List<Cell> parsed = fromBoc(java.util.Base64.getDecoder().decode(src));
        if (parsed.size() != 1) {
            throw new IllegalStateException("Deserialized more than one cell");
        }
        return parsed.get(0);
    }

    /* ============================================================ */
    /* ======================= public fields ====================== */
    /* ============================================================ */

    public final CellType type;
    public final BitString bits;
    public final List<Cell> refs;
    public final LevelMask mask;

    /* ============================================================ */
    /* ======================= internal fields ==================== */
    /* ============================================================ */

    private final byte[][] hashes;
    private final int[] depths;

    /* ============================================================ */
    /* ======================= constructors ======================= */
    /* ============================================================ */

    public Cell() {
        this(null);
    }

    public Cell(Options opts) {

        // Resolve bits
        BitString bits = BitString.EMPTY;
        if (opts != null && opts.bits != null) {
            bits = opts.bits;
        }

        // Resolve refs
        List<Cell> refs = new ArrayList<>();
        if (opts != null && opts.refs != null) {
            refs.addAll(opts.refs);
        }

        CellType type;
        LevelMask mask;
        byte[][] hashes;
        int[] depths;

        if (opts != null && opts.exotic) {

            // Resolve exotic cell
            ResolveExotic.Result resolved = ResolveExotic.resolve(bits, refs);

            // Perform wonders
            WonderCalculator.Result wonders =
                    WonderCalculator.calculate(resolved.type, bits, refs);

            type = resolved.type;
            mask = wonders.mask;
            hashes = wonders.hashes;
            depths = wonders.depths;

        } else {

            // Check correctness
            if (refs.size() > 4) {
                throw new IllegalStateException("Invalid number of references");
            }
            if (bits.length() > 1023) {
                throw new IllegalStateException("Bits overflow: " + bits.length() + " > 1023");
            }

            // Perform wonders
            WonderCalculator.Result wonders =
                    WonderCalculator.calculate(CellType.Ordinary, bits, refs);

            type = CellType.Ordinary;
            mask = wonders.mask;
            hashes = wonders.hashes;
            depths = wonders.depths;
        }

        this.type = type;
        this.bits = bits;
        this.refs = Collections.unmodifiableList(refs);
        this.mask = mask;
        this.hashes = hashes;
        this.depths = depths;
    }

    /* ============================================================ */
    /* ======================= properties ========================= */
    /* ============================================================ */

    public boolean isExotic() {
        return type != CellType.Ordinary;
    }

    /* ============================================================ */
    /* ======================= parsing ============================ */
    /* ============================================================ */

    public Slice beginParse() {
        return beginParse(false);
    }

    public Slice beginParse(boolean allowExotic) {
        if (isExotic() && !allowExotic) {
            throw new IllegalStateException("Exotic cells cannot be parsed");
        }
        return new Slice(new BitReader(bits), refs);
    }

    /* ============================================================ */
    /* ======================= hash / depth ======================= */
    /* ============================================================ */

    public byte[] hash() {
        return hash(3);
    }

    public byte[] hash(int level) {
        int idx = Math.min(hashes.length - 1, level);
        return hashes[idx];
    }

    public int depth() {
        return depth(3);
    }

    public int depth(int level) {
        int idx = Math.min(depths.length - 1, level);
        return depths[idx];
    }

    public int level() {
        return mask.level();
    }

    /* ============================================================ */
    /* ======================= equality ============================ */
    /* ============================================================ */

    public boolean equals(Cell other) {
        return java.util.Arrays.equals(this.hash(), other.hash());
    }

    /* ============================================================ */
    /* ======================= serialization ====================== */
    /* ============================================================ */

    public byte[] toBoc() {
        return toBoc(null);
    }

    public byte[] toBoc(SerializeOptions opts) {
        boolean idx = opts != null && opts.idx != null ? opts.idx : false;
        boolean crc32 = opts != null && opts.crc32 != null ? opts.crc32 : true;
        return BocSerialization.serializeBoc(this, idx, crc32);
    }

    /* ============================================================ */
    /* ======================= conversions ======================== */
    /* ============================================================ */

    public Slice asSlice() {
        return beginParse();
    }

    public Builder asBuilder() {
        return Builder.beginCell().storeSlice(asSlice());
    }

    /* ============================================================ */
    /* ======================= string ============================= */
    /* ============================================================ */

    public static Cell exotic(BitString bits, List<Cell> refs) {
        Options o = new Options();
        o.exotic = true;
        o.bits = bits;
        o.refs = refs;
        return new Cell(o);
    }

    public static Cell ordinary(BitString bits, List<Cell> refs) {
        Options o = new Options();
        o.exotic = false;
        o.bits = bits;
        o.refs = refs;
        return new Cell(o);
    }

    @Override
    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        String id = indent != null ? indent : "";
        String t = "x";

        if (isExotic()) {
            if (type == CellType.MerkleProof) {
                t = "p";
            } else if (type == CellType.MerkleUpdate) {
                t = "u";
            } else if (type == CellType.PrunedBranch) {
                t = "p";
            }
        }

        StringBuilder s = new StringBuilder();
        s.append(id).append(t).append('{').append(bits.toString()).append('}');

        for (Cell ref : refs) {
            s.append('\n').append(ref.toString(id + " "));
        }

        return s.toString();
    }

    /* ============================================================ */
    /* ======================= option records ===================== */
    /* ============================================================ */

    public static final class Options {
        public boolean exotic;
        public BitString bits;
        public List<Cell> refs;
    }

    public static final class SerializeOptions {
        public Boolean idx;
        public Boolean crc32;
    }

    public int bitsLength() {
        return bits.length();
    }
    public int refsCount() {
        return refs.size();
    }
}
