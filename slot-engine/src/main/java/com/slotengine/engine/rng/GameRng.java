package com.slotengine.engine.rng;

/**
 * Source of randomness for a single round. Implementations must be isolated per round:
 * the engine never shares an RNG across concurrent plays.
 */
public interface GameRng {

    /** Uniform integer in {@code [0, bound)}. */
    int nextInt(int bound);

    /** Uniform double in {@code [0, 1)}. */
    double nextDouble();

    long seed();
}
