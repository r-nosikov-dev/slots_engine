package com.slotengine.model;

public record GridSize(int reels, int rows) {

    public GridSize {
        if (reels < 1 || reels > 12) {
            throw new IllegalArgumentException("reels must be 1..12, got " + reels);
        }
        if (rows < 1 || rows > 12) {
            throw new IllegalArgumentException("rows must be 1..12, got " + rows);
        }
    }

    public int cells() {
        return reels * rows;
    }

    /** All-ways combination count, e.g. 5×3 → 243, 5×4 → 1024. */
    public int ways() {
        int ways = 1;
        for (int i = 0; i < reels; i++) {
            ways *= rows;
        }
        return ways;
    }

    @Override
    public String toString() {
        return reels + "x" + rows;
    }
}
