package dev.quark.ton.core.utils;

public final class Prando {

    private static final int MIN = 0x80000000; // -2147483648
    private static final int MAX = 0x7fffffff; //  2147483647

    private final int seed;
    private int value;

    public Prando(String seed) {
        this.seed = hashCode(seed);
        reset();
    }

    public Prando(int seed) {
        this.seed = seed;
        reset();
    }

    public void reset() {
        this.value = this.seed;
    }

    public double next(double min, double pseudoMax) {
        recalculate();
        return map(value, MIN, MAX, min, pseudoMax);
    }

    public int nextInt(int min, int max) {
        recalculate();
        // TS: Math.floor(map(this._value, MIN, MAX, min, max + 1))
        return (int) Math.floor(map(value, MIN, MAX, min, (double) max + 1.0d));
    }

    private void recalculate() {
        // Xorshift*32
        value ^= (value << 13);
        value ^= (value >> 17);
        value ^= (value << 5);
    }

    private static double map(int val, int minFrom, int maxFrom, double minTo, double maxTo) {
        long numerator = (long) val - (long) minFrom;     // 0..4294967295
        long denom = (long) maxFrom - (long) minFrom;     // 4294967295
        return ((double) numerator / (double) denom) * (maxTo - minTo) + minTo;
    }

    private static int hashCode(String str) {
        int hash = 0;
        if (str != null && !str.isEmpty()) {
            for (int i = 0; i < str.length(); i++) {
                hash = ((hash << 5) - hash) + str.charAt(i); // hash*31 + char
            }
        }
        return hash; // already 32-bit
    }
}
