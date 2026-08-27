package com.slotengine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ReelSet {

    private final String name;
    private final List<ReelStrip> reels;

    public ReelSet(String name, List<ReelStrip> reels) {
        this.name = Objects.requireNonNull(name, "name");
        if (reels == null || reels.isEmpty()) {
            throw new IllegalArgumentException("reel set must contain at least one reel");
        }
        this.reels = List.copyOf(reels);
    }

    public static ReelSet of(String name, ReelStrip... reels) {
        return new ReelSet(name, List.of(reels));
    }

    public String name() {
        return name;
    }

    public List<ReelStrip> reels() {
        return reels;
    }

    public int reelCount() {
        return reels.size();
    }

    public ReelStrip reel(int index) {
        return reels.get(index);
    }

    /**
     * Full cycle {@code Π Li} — number of equally likely stop tuples.
     * Exact base RTP enumerates all of them.
     */
    public long cycleSize() {
        long cycle = 1L;
        for (ReelStrip reel : reels) {
            long next = Math.multiplyExact(cycle, reel.length());
            cycle = next;
        }
        return cycle;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private final List<ReelStrip> reels = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder reel(String... symbols) {
            reels.add(ReelStrip.of(symbols));
            return this;
        }

        public Builder reel(ReelStrip strip) {
            reels.add(strip);
            return this;
        }

        public Builder reelWeights(java.util.Map<String, Integer> weights) {
            reels.add(ReelStrip.fromWeights(weights));
            return this;
        }

        public ReelSet build() {
            return new ReelSet(name, reels);
        }
    }
}
