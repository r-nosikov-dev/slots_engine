package com.slotengine.engine;

import com.slotengine.model.PlayMode;

import java.util.List;
import java.util.UUID;

public record RoundResult(
        String roundId,
        String gameId,
        PlayMode mode,
        long seed,
        long totalBet,
        long charged,
        List<SpinSnapshot> spins,
        long totalWin,
        boolean maxWinCapped,
        List<FeatureTrigger> triggers
) {

    public RoundResult {
        spins = spins == null ? List.of() : List.copyOf(spins);
        triggers = triggers == null ? List.of() : List.copyOf(triggers);
        if (roundId == null || roundId.isBlank()) {
            roundId = UUID.randomUUID().toString();
        }
    }

    public boolean isWin() {
        return totalWin > 0;
    }

    public boolean triggeredFeature() {
        return !triggers.isEmpty();
    }
}
