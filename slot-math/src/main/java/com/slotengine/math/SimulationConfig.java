package com.slotengine.math;

import com.slotengine.model.PlayMode;

public record SimulationConfig(
        long spins,
        long totalBet,
        long seed,
        PlayMode mode,
        int progressEvery
) {

    public SimulationConfig {
        if (spins < 1) {
            throw new IllegalArgumentException("spins must be >= 1");
        }
        if (totalBet < 1) {
            throw new IllegalArgumentException("totalBet must be >= 1");
        }
        if (mode == null) {
            mode = PlayMode.NORMAL;
        }
        if (progressEvery < 0) {
            progressEvery = 0;
        }
    }

    public static SimulationConfig of(long spins, long totalBet) {
        return new SimulationConfig(spins, totalBet, 1L, PlayMode.NORMAL, 0);
    }
}
