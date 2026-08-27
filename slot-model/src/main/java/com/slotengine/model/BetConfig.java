package com.slotengine.model;

import java.util.List;
import java.util.Objects;

/**
 * Coin-value ladder. For payline games total bet = coinValue × coinsPerLine × lineCount.
 * For ways games total bet is an explicit stake chosen from the ladder of legal totals.
 */
public record BetConfig(
        List<Long> coinValues,
        long defaultCoinValue,
        int defaultCoinsPerLine,
        long minTotalBet,
        long maxTotalBet
) {

    public BetConfig {
        coinValues = List.copyOf(Objects.requireNonNull(coinValues, "coinValues"));
        if (coinValues.isEmpty()) {
            throw new IllegalArgumentException("coinValues must not be empty");
        }
        for (Long v : coinValues) {
            if (v == null || v <= 0) {
                throw new IllegalArgumentException("coin values must be positive");
            }
        }
        if (!coinValues.contains(defaultCoinValue)) {
            throw new IllegalArgumentException("defaultCoinValue must be one of coinValues");
        }
        if (defaultCoinsPerLine < 1) {
            throw new IllegalArgumentException("defaultCoinsPerLine must be >= 1");
        }
        if (minTotalBet <= 0 || maxTotalBet < minTotalBet) {
            throw new IllegalArgumentException("invalid bet range");
        }
    }

    public static BetConfig credits(long... coins) {
        List<Long> values = new java.util.ArrayList<>();
        for (long c : coins) {
            values.add(c);
        }
        long min = values.get(0);
        long max = values.get(values.size() - 1) * 100;
        return new BetConfig(values, values.get(0), 1, min, max);
    }

    public boolean isAllowedCoin(long coinValue) {
        return coinValues.contains(coinValue);
    }
}
