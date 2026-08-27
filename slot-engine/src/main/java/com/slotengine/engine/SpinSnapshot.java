package com.slotengine.engine;

import com.slotengine.engine.eval.Win;
import com.slotengine.engine.eval.Window;

import java.util.List;

public record SpinSnapshot(
        int index,
        SpinPhase phase,
        Window window,
        List<Win> wins,
        long winAmount,
        int multiplier,
        List<FeatureTrigger> triggers,
        int freeSpinsRemaining
) {

    public SpinSnapshot {
        wins = wins == null ? List.of() : List.copyOf(wins);
        triggers = triggers == null ? List.of() : List.copyOf(triggers);
    }
}
