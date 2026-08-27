package com.slotengine.engine.rng;

/**
 * Deterministic xorshift64* generator. Same seed always yields the same sequence,
 * including across JVMs — required for round replay and Monte-Carlo checksums.
 *
 * <p>Algorithm: Marsaglia xorshift64* with scrambler {@code 0x2545F4914F6CDD1D}.
 * {@link #nextInt(int)} uses rejection sampling so every residue in {@code [0, bound)}
 * is equally likely.
 */
public final class SeededGameRng implements GameRng {

    private static final long MULTIPLIER = 0x2545F4914F6CDD1DL;

    private long state;
    private final long seed;

    public SeededGameRng(long seed) {
        this.seed = seed;
        this.state = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        // Unbiased remainder: reject values in the incomplete modulo bucket.
        long modulus = 1L << 32;
        long threshold = modulus % bound;
        long r;
        do {
            r = nextLong() & 0xFFFFFFFFL;
        } while (r < threshold);
        return (int) (r % bound);
    }

    @Override
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    @Override
    public long seed() {
        return seed;
    }

    /** One xorshift64* step; output is the scrambled 64-bit state. */
    public long nextLong() {
        long x = state;
        x ^= x >>> 12;
        x ^= x << 25;
        x ^= x >>> 27;
        state = x;
        return x * MULTIPLIER;
    }
}
