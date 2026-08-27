package com.slotengine.math;

import com.slotengine.model.PlayMode;
import com.slotengine.model.VolatilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

public record SimulationReport(
        String gameId,
        PlayMode mode,
        long spins,
        long totalBet,
        long seed,
        long totalWagered,
        long totalWon,
        long hits,
        long featureHits,
        long maxWin,
        double maxWinX,
        double variance,
        double stddev,
        VolatilityClass observedVolatility,
        Map<String, Long> payoutBuckets,
        long elapsedMs
) {

    /** {@code totalWon / totalWagered} including free spins, buy-bonus cost and max-win cap. */
    public BigDecimal rtp() {
        if (totalWagered == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalWon)
                .divide(BigDecimal.valueOf(totalWagered), 6, RoundingMode.HALF_UP);
    }

    /** Fraction of rounds with {@code totalWin > 0}. */
    public BigDecimal hitFrequency() {
        if (spins == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(hits)
                .divide(BigDecimal.valueOf(spins), 6, RoundingMode.HALF_UP);
    }

    /** Fraction of rounds that opened a feature (free spins / jackpot trigger). */
    public BigDecimal featureFrequency() {
        if (spins == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(featureHits)
                .divide(BigDecimal.valueOf(spins), 6, RoundingMode.HALF_UP);
    }

    /** Average rounds between feature hits: {@code spins / featureHits}. */
    public double featureOneIn() {
        return featureHits == 0 ? Double.POSITIVE_INFINITY : (double) spins / featureHits;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("gameId", gameId);
        map.put("mode", mode.name());
        map.put("spins", spins);
        map.put("totalBet", totalBet);
        map.put("seed", seed);
        map.put("rtp", rtp());
        map.put("rtpPercent", rtp().movePointRight(2).setScale(4, RoundingMode.HALF_UP) + "%");
        map.put("hitFrequency", hitFrequency());
        map.put("hitFrequencyPercent", hitFrequency().movePointRight(2).setScale(2, RoundingMode.HALF_UP) + "%");
        map.put("featureFrequency", featureFrequency());
        map.put("featureOneIn", featureHits == 0 ? null : round(featureOneIn(), 2));
        map.put("maxWin", maxWin);
        map.put("maxWinX", round(maxWinX, 2));
        map.put("stddev", round(stddev, 4));
        map.put("variance", round(variance, 4));
        map.put("observedVolatility", observedVolatility.name());
        map.put("payoutBuckets", payoutBuckets);
        map.put("elapsedMs", elapsedMs);
        return map;
    }

    private static double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
