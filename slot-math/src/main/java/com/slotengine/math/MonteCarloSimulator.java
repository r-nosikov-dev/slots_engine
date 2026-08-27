package com.slotengine.math;

import com.slotengine.engine.PlayRequest;
import com.slotengine.engine.RoundResult;
import com.slotengine.engine.SlotEngine;
import com.slotengine.model.GameDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Full-game Monte Carlo: every round goes through {@link SlotEngine}, so free spins,
 * cascades, buy-bonus and max-win cap are all included. Use a fixed seed for
 * reproducible PAR-sheet numbers.
 */
public final class MonteCarloSimulator {

    private final SlotEngine engine;

    public MonteCarloSimulator() {
        this(new SlotEngine());
    }

    public MonteCarloSimulator(SlotEngine engine) {
        this.engine = engine;
    }

    /**
     * RTP = {@code totalWon / totalWagered}. Hit frequency = rounds with win &gt; 0.
     * Variance is Welford's online variance of {@code win / charged} per round (sample, n-1).
     */
    public SimulationReport simulate(GameDefinition game, SimulationConfig config) {
        long start = System.nanoTime();
        long wagered = 0;
        long won = 0;
        long hits = 0;
        long features = 0;
        long maxWin = 0;
        // Welford mean/variance of win/bet.
        double mean = 0;
        double m2 = 0;
        Map<String, Long> buckets = emptyBuckets();

        for (long i = 0; i < config.spins(); i++) {
            long seed = config.seed() + i;
            RoundResult result = engine.play(
                    game,
                    new PlayRequest(config.totalBet(), config.mode(), java.util.OptionalLong.of(seed))
            );
            wagered += result.charged();
            won += result.totalWin();
            if (result.totalWin() > 0) {
                hits++;
            }
            if (result.triggeredFeature()) {
                features++;
            }
            if (result.totalWin() > maxWin) {
                maxWin = result.totalWin();
            }
            double x = result.charged() == 0 ? 0 : (double) result.totalWin() / result.charged();
            double delta = x - mean;
            mean += delta / (i + 1);
            m2 += delta * (x - mean);
            buckets.merge(VolatilityIndex.bucket(x), 1L, Long::sum);
        }

        double variance = config.spins() > 1 ? m2 / (config.spins() - 1) : 0;
        double stddev = Math.sqrt(variance);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        return new SimulationReport(
                game.id(),
                config.mode(),
                config.spins(),
                config.totalBet(),
                config.seed(),
                wagered,
                won,
                hits,
                features,
                maxWin,
                config.totalBet() == 0 ? 0 : (double) maxWin / config.totalBet(),
                variance,
                stddev,
                VolatilityIndex.classify(stddev),
                buckets,
                elapsedMs
        );
    }

    private static Map<String, Long> emptyBuckets() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (String key : new String[]{"0", "0-1x", "1-5x", "5-15x", "15-50x", "50-100x", "100-500x", "500x+"}) {
            map.put(key, 0L);
        }
        return map;
    }
}
