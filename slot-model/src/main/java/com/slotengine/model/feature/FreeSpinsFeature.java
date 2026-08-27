package com.slotengine.model.feature;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public record FreeSpinsFeature(
        String triggerSymbol,
        int minTriggerCount,
        NavigableMap<Integer, Integer> awards,
        int retriggerSpins,
        boolean retriggerEnabled,
        int multiplier,
        boolean useFreeReels,
        int maxAwardedSpins,
        boolean stickyWilds
) {

    public FreeSpinsFeature {
        Objects.requireNonNull(triggerSymbol, "triggerSymbol");
        if (minTriggerCount < 1) {
            throw new IllegalArgumentException("minTriggerCount must be >= 1");
        }
        awards = awards == null ? new TreeMap<>() : new TreeMap<>(awards);
        awards = java.util.Collections.unmodifiableNavigableMap(awards);
        if (awards.isEmpty()) {
            throw new IllegalArgumentException("free spins awards must not be empty");
        }
        if (multiplier < 1) {
            throw new IllegalArgumentException("free-spin multiplier must be >= 1");
        }
        if (maxAwardedSpins < 1) {
            throw new IllegalArgumentException("maxAwardedSpins must be >= 1");
        }
        if (retriggerSpins < 0) {
            throw new IllegalArgumentException("retriggerSpins must be >= 0");
        }
    }

    public int spinsFor(int scatterCount) {
        if (scatterCount < minTriggerCount) {
            return 0;
        }
        Map.Entry<Integer, Integer> floor = awards.floorEntry(scatterCount);
        return floor == null ? 0 : floor.getValue();
    }

    public static Builder builder(String triggerSymbol) {
        return new Builder(triggerSymbol);
    }

    public static final class Builder {
        private final String triggerSymbol;
        private int minTriggerCount = 3;
        private final NavigableMap<Integer, Integer> awards = new TreeMap<>();
        private int retriggerSpins = 0;
        private boolean retriggerEnabled = true;
        private int multiplier = 1;
        private boolean useFreeReels = false;
        private int maxAwardedSpins = 300;
        private boolean stickyWilds = false;

        private Builder(String triggerSymbol) {
            this.triggerSymbol = triggerSymbol;
        }

        public Builder minTriggerCount(int count) {
            this.minTriggerCount = count;
            return this;
        }

        public Builder award(int scatterCount, int spins) {
            awards.put(scatterCount, spins);
            return this;
        }

        public Builder retrigger(boolean enabled, int spins) {
            this.retriggerEnabled = enabled;
            this.retriggerSpins = spins;
            return this;
        }

        public Builder multiplier(int multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder useFreeReels(boolean use) {
            this.useFreeReels = use;
            return this;
        }

        public Builder stickyWilds(boolean sticky) {
            this.stickyWilds = sticky;
            return this;
        }

        public Builder maxAwardedSpins(int max) {
            this.maxAwardedSpins = max;
            return this;
        }

        public FreeSpinsFeature build() {
            int retrigger = retriggerSpins;
            if (retriggerEnabled && retrigger == 0 && !awards.isEmpty()) {
                retrigger = awards.firstEntry().getValue();
            }
            return new FreeSpinsFeature(
                    triggerSymbol,
                    minTriggerCount,
                    awards,
                    retrigger,
                    retriggerEnabled,
                    multiplier,
                    useFreeReels,
                    maxAwardedSpins,
                    stickyWilds
            );
        }
    }

    public Optional<String> trigger() {
        return Optional.of(triggerSymbol);
    }
}
