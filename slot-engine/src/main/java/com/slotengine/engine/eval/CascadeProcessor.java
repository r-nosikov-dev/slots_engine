package com.slotengine.engine.eval;

import com.slotengine.model.GameDefinition;
import com.slotengine.model.Position;
import com.slotengine.model.ReelSet;
import com.slotengine.model.feature.FeatureSet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tumble / avalanche: winning symbols disappear, remaining symbols fall down,
 * empty cells are filled from the strip above the current stop.
 */
public final class CascadeProcessor {

    /**
     * Gravity toward the bottom row. New symbols come from {@code stop - pulledFromAbove}
     * on the same physical strip, so cascade fill is deterministic given the original stop.
     */
    public Window drop(GameDefinition game, ReelSet reels, Window window, List<Win> wins, int[] pulledFromAbove) {
        Set<Long> remove = new HashSet<>();
        for (Win win : wins) {
            if (win.type() == WinType.SCATTER || win.type() == WinType.JACKPOT) {
                continue;
            }
            for (Position p : win.positions()) {
                remove.add(key(p.reel(), p.row()));
            }
        }
        if (remove.isEmpty()) {
            return window;
        }

        String[][] next = new String[window.reels()][window.rows()];
        int[] stops = window.stops();
        for (int reel = 0; reel < window.reels(); reel++) {
            java.util.ArrayDeque<String> kept = new java.util.ArrayDeque<>();
            for (int row = window.rows() - 1; row >= 0; row--) {
                if (!remove.contains(key(reel, row))) {
                    kept.addFirst(window.at(reel, row));
                }
            }
            int missing = window.rows() - kept.size();
            for (int i = 0; i < missing; i++) {
                pulledFromAbove[reel]++;
                int stripPos = stops[reel] - pulledFromAbove[reel];
                kept.addFirst(reels.reel(reel).at(stripPos));
            }
            int row = 0;
            for (String id : kept) {
                next[reel][row++] = id;
            }
        }
        return new Window(next, stops);
    }

    /** Next cascade multiplier: {@code min(max, current + increment)}. */
    public static int nextMultiplier(FeatureSet.CascadeFeature feature, int current) {
        if (!feature.enabled()) {
            return 1;
        }
        return Math.min(feature.maxMultiplier(), current + feature.increment());
    }

    private static long key(int reel, int row) {
        return (((long) reel) << 32) | (row & 0xFFFFFFFFL);
    }
}
