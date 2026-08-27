package com.slotengine.engine.rng;

import java.security.SecureRandom;

/** Cryptographic seed source for live play. The round itself still runs on {@link SeededGameRng}. */
public final class SecureSeeds {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureSeeds() {
    }

    public static long next() {
        long seed = RANDOM.nextLong();
        return seed == 0 ? 1L : seed;
    }
}
