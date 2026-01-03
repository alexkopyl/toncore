package dev.quark.ton.core.test;

public final class Prando {

    private static final int MIN = 0x80000000; // -2147483648
    private static final int MAX = 0x7fffffff; //  2147483647

    private final int seed;
    private int value;

    public Prando(String seed) {
        this.seed = getSafeSeed(hashCode(seed));
        reset();
    }

    public Prando(int seed) {
        this.seed = getSafeSeed(seed);
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
        return (int) Math.floor(map(value, MIN, MAX, min, (double) max + 1.0d));
    }

    private void recalculate() {
        value = xorshift(value);
    }

    private static int xorshift(int v) {
        v ^= (v << 13);
        v ^= (v >> 17);
        v ^= (v << 5);
        return v;
    }

    private static double map(int val, int minFrom, int maxFrom, double minTo, double maxTo) {
        // как в TS: ((val - minFrom) / (maxFrom - minFrom)) * (maxTo-minTo) + minTo
        return (((double) ((long) val - (long) minFrom)) / (double) ((long) maxFrom - (long) minFrom)) * (maxTo - minTo) + minTo;
    }

    private static int hashCode(String str) {
        int hash = 0;
        if (str != null && !str.isEmpty()) {
            for (int i = 0; i < str.length(); i++) {
                hash = (hash << 5) - hash + str.charAt(i);
                // TS: hash |= 0  => уже int
                hash = xorshift(hash); // ✅ как в TS
            }
        }
        return hash;
    }

    private static int getSafeSeed(int seed) {
        return seed == 0 ? 1 : seed;
    }
}

