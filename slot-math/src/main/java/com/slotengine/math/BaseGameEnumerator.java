package com.slotengine.math;

import com.slotengine.engine.eval.PaylineEvaluator;
import com.slotengine.engine.eval.ScatterEvaluator;
import com.slotengine.engine.eval.WaysEvaluator;
import com.slotengine.engine.eval.Win;
import com.slotengine.engine.eval.Window;
import com.slotengine.model.EvaluationMode;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.ReelSet;
import com.slotengine.model.feature.FreeSpinsFeature;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact cycle walk of the base reel set (no free-spin play). Feasible when
 * Π strip lengths is in the low tens of millions. Reports line/ways RTP,
 * scatter RTP and raw feature-trigger rate.
 */
public final class BaseGameEnumerator {

    private static final long DEFAULT_LIMIT = 50_000_000L;

    private final PaylineEvaluator paylines = new PaylineEvaluator();
    private final WaysEvaluator ways = new WaysEvaluator();
    private final ScatterEvaluator scatters = new ScatterEvaluator();

    public EnumerationReport enumerate(GameDefinition game, long totalBet) {
        return enumerate(game, totalBet, DEFAULT_LIMIT);
    }

    /**
     * Walks every stop tuple {@code (s0,…,s_{n-1})} with {@code si ∈ [0, Li)}.
     * Base RTP = {@code (lineWon + scatterWon) / (cycle × totalBet)}.
     * Free-spin EV is excluded; use {@link MonteCarloSimulator} for full RTP.
     */
    public EnumerationReport enumerate(GameDefinition game, long totalBet, long cycleLimit) {
        ReelSet reels = game.baseReels();
        long cycle = reels.cycleSize();
        if (cycle > cycleLimit) {
            throw new IllegalArgumentException(
                    "Cycle " + cycle + " exceeds limit " + cycleLimit + " — use Monte Carlo instead"
            );
        }
        game.assertBetLegal(totalBet);
        int n = reels.reelCount();
        int[] lengths = new int[n];
        for (int i = 0; i < n; i++) {
            lengths[i] = reels.reel(i).length();
        }
        int[] stops = new int[n];
        long lineWon = 0;
        long scatterWon = 0;
        long hits = 0;
        long featureHits = 0;
        long start = System.nanoTime();

        if (n == 0) {
            throw new IllegalArgumentException("no reels");
        }
        while (true) {
            Window window = Window.fromStops(reels, stops, game.grid().rows());
            List<Win> evaluated = game.evaluation() == EvaluationMode.WAYS
                    ? ways.evaluate(game, window, totalBet, 1)
                    : paylines.evaluate(game, window, totalBet, 1);
            List<Win> scatterWins = scatters.evaluate(game, window, totalBet);
            long line = 0;
            for (Win win : evaluated) {
                line += win.amount();
            }
            long scat = 0;
            for (Win win : scatterWins) {
                scat += win.amount();
            }
            lineWon += line;
            scatterWon += scat;
            if (line + scat > 0) {
                hits++;
            }
            if (triggered(game, window)) {
                featureHits++;
            }
            if (!increment(stops, lengths)) {
                break;
            }
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        long wagered = Math.multiplyExact(cycle, totalBet);
        return new EnumerationReport(
                game.id(),
                cycle,
                totalBet,
                wagered,
                lineWon,
                scatterWon,
                hits,
                featureHits,
                elapsedMs
        );
    }

    private static boolean triggered(GameDefinition game, Window window) {
        FreeSpinsFeature fs = game.features().freeSpins().orElse(null);
        if (fs == null) {
            return false;
        }
        return window.count(fs.triggerSymbol()) >= fs.minTriggerCount();
    }

    private static boolean increment(int[] stops, int[] lengths) {
        for (int i = stops.length - 1; i >= 0; i--) {
            stops[i]++;
            if (stops[i] < lengths[i]) {
                return true;
            }
            stops[i] = 0;
        }
        return false;
    }

    public record EnumerationReport(
            String gameId,
            long cycle,
            long totalBet,
            long wagered,
            long lineWon,
            long scatterWon,
            long hits,
            long featureHits,
            long elapsedMs
    ) {
        /** Line/ways contribution: {@code lineWon / (cycle × bet)}. */
        public BigDecimal lineRtp() {
            return div(lineWon, wagered);
        }

        /** Scatter contribution: {@code scatterWon / (cycle × bet)}. */
        public BigDecimal scatterRtp() {
            return div(scatterWon, wagered);
        }

        /** {@code lineRtp + scatterRtp}. Does not include free-spin EV. */
        public BigDecimal baseRtp() {
            return div(lineWon + scatterWon, wagered);
        }

        /** Share of stop combinations with a non-zero line or scatter win. */
        public BigDecimal hitFrequency() {
            return div(hits, cycle);
        }

        /** Share of combinations that would trigger free spins (scatter count ≥ min). */
        public BigDecimal featureFrequency() {
            return div(featureHits, cycle);
        }

        public Map<String, Object> asMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("gameId", gameId);
            map.put("cycle", cycle);
            map.put("lineRtp", lineRtp());
            map.put("scatterRtp", scatterRtp());
            map.put("baseRtp", baseRtp());
            map.put("hitFrequency", hitFrequency());
            map.put("featureFrequency", featureFrequency());
            map.put("elapsedMs", elapsedMs);
            map.put("notes", "Base game only — free-spin EV is not included. Use Monte Carlo for full RTP.");
            return map;
        }

        private static BigDecimal div(long a, long b) {
            if (b == 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), 8, RoundingMode.HALF_UP);
        }
    }
}
