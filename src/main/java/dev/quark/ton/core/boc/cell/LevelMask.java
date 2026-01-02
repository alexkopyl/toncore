package dev.quark.ton.core.boc.cell;

/**
 * Ported 1:1 from ton-core/src/boc/cell/LevelMask.ts
 */
public final class LevelMask {

    private final int mask;
    private final int hashIndex;
    private final int hashCount;

    public LevelMask() {
        this(0);
    }

    public LevelMask(int mask) {
        this.mask = mask;
        this.hashIndex = countSetBits(this.mask);
        this.hashCount = this.hashIndex + 1;
    }

    public int value() {
        return mask;
    }

    /**
     * TS: 32 - Math.clz32(mask)
     * Java equivalent: 32 - Integer.numberOfLeadingZeros(mask)
     */
    public int level() {
        if (mask == 0) {
            return 0;
        }
        return 32 - Integer.numberOfLeadingZeros(mask);
    }

    public int hashIndex() {
        return hashIndex;
    }

    public int hashCount() {
        return hashCount;
    }

    /**
     * apply(level): mask & ((1 << level) - 1)
     */
    public LevelMask apply(int level) {
        return new LevelMask(mask & ((1 << level) - 1));
    }

    /**
     * level === 0 || ((mask >> (level - 1)) % 2 !== 0)
     */
    public boolean isSignificant(int level) {
        return level == 0 || ((mask >> (level - 1)) & 1) != 0;
    }

    /* ============================================================ */
    /* ======================= helpers ============================ */
    /* ============================================================ */

    /**
     * Port of countSetBits(n: number)
     *
     * Bit-twiddling hack, identical to TS version.
     */
    private static int countSetBits(int n) {
        n = n - ((n >> 1) & 0x55555555);
        n = (n & 0x33333333) + ((n >> 2) & 0x33333333);
        return ((n + (n >> 4) & 0x0F0F0F0F) * 0x01010101) >>> 24;
    }
}
