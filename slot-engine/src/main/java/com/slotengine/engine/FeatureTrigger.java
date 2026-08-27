package com.slotengine.engine;

import java.util.List;

public record FeatureTrigger(String type, String symbolId, int count, int awardedSpins, List<String> details) {

    public static FeatureTrigger freeSpins(String symbolId, int count, int awarded) {
        return new FeatureTrigger("FREE_SPINS", symbolId, count, awarded, List.of());
    }

    public static FeatureTrigger retrigger(String symbolId, int count, int awarded) {
        return new FeatureTrigger("RETRIGGER", symbolId, count, awarded, List.of());
    }

    public static FeatureTrigger jackpot(String tierId, long amount) {
        return new FeatureTrigger("JACKPOT", tierId, 1, 0, List.of(Long.toString(amount)));
    }
}
