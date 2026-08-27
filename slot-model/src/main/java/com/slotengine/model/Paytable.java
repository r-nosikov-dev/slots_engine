package com.slotengine.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Multipliers keyed by symbol id and of-a-kind count.
 * Line pays use {@link PayUnit#LINE_BET}; scatter/ways pays typically use {@link PayUnit#TOTAL_BET}.
 */
public final class Paytable {

    private final PayUnit lineUnit;
    private final PayUnit scatterUnit;
    private final Map<String, NavigableMap<Integer, Integer>> linePays;
    private final Map<String, NavigableMap<Integer, Integer>> scatterPays;

    private Paytable(
            PayUnit lineUnit,
            PayUnit scatterUnit,
            Map<String, NavigableMap<Integer, Integer>> linePays,
            Map<String, NavigableMap<Integer, Integer>> scatterPays
    ) {
        this.lineUnit = Objects.requireNonNull(lineUnit);
        this.scatterUnit = Objects.requireNonNull(scatterUnit);
        this.linePays = freeze(linePays);
        this.scatterPays = freeze(scatterPays);
    }

    public static Builder builder() {
        return new Builder();
    }

    public PayUnit lineUnit() {
        return lineUnit;
    }

    public PayUnit scatterUnit() {
        return scatterUnit;
    }

    public Map<String, NavigableMap<Integer, Integer>> linePays() {
        return linePays;
    }

    public Map<String, NavigableMap<Integer, Integer>> scatterPays() {
        return scatterPays;
    }

    /** Paytable multiplier for {@code ofAKind} of {@code symbolId}, or 0 if that count does not pay. */
    public int linePay(String symbolId, int ofAKind) {
        return lookup(linePays, symbolId, ofAKind);
    }

    /** Scatter multiplier for an anywhere-count; applied to {@link #scatterUnit()}. */
    public int scatterPay(String symbolId, int ofAKind) {
        return lookup(scatterPays, symbolId, ofAKind);
    }

    public int minOfAKind(String symbolId) {
        NavigableMap<Integer, Integer> row = linePays.get(symbolId);
        if (row == null || row.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return row.firstKey();
    }

    public boolean hasLinePays(String symbolId) {
        NavigableMap<Integer, Integer> row = linePays.get(symbolId);
        return row != null && !row.isEmpty();
    }

    private static int lookup(Map<String, NavigableMap<Integer, Integer>> table, String symbolId, int ofAKind) {
        NavigableMap<Integer, Integer> row = table.get(symbolId);
        if (row == null) {
            return 0;
        }
        Integer pay = row.get(ofAKind);
        return pay == null ? 0 : pay;
    }

    private static Map<String, NavigableMap<Integer, Integer>> freeze(
            Map<String, NavigableMap<Integer, Integer>> source
    ) {
        Map<String, NavigableMap<Integer, Integer>> copy = new LinkedHashMap<>();
        source.forEach((id, row) -> copy.put(id, Collections.unmodifiableNavigableMap(new TreeMap<>(row))));
        return Collections.unmodifiableMap(copy);
    }

    public static final class Builder {
        private PayUnit lineUnit = PayUnit.LINE_BET;
        private PayUnit scatterUnit = PayUnit.TOTAL_BET;
        private final Map<String, NavigableMap<Integer, Integer>> linePays = new LinkedHashMap<>();
        private final Map<String, NavigableMap<Integer, Integer>> scatterPays = new LinkedHashMap<>();

        public Builder lineUnit(PayUnit unit) {
            this.lineUnit = unit;
            return this;
        }

        public Builder scatterUnit(PayUnit unit) {
            this.scatterUnit = unit;
            return this;
        }

        /** {@code pays("LYNX", 3, 30, 4, 100, 5, 500)} */
        public Builder line(String symbolId, int... countThenMultiplier) {
            put(linePays, symbolId, countThenMultiplier);
            return this;
        }

        public Builder scatter(String symbolId, int... countThenMultiplier) {
            put(scatterPays, symbolId, countThenMultiplier);
            return this;
        }

        public Paytable build() {
            return new Paytable(lineUnit, scatterUnit, linePays, scatterPays);
        }

        private static void put(Map<String, NavigableMap<Integer, Integer>> target, String symbolId, int[] pairs) {
            if (pairs.length == 0 || pairs.length % 2 != 0) {
                throw new IllegalArgumentException("pay entries must be count,multiplier pairs for " + symbolId);
            }
            NavigableMap<Integer, Integer> row = target.computeIfAbsent(symbolId, k -> new TreeMap<>());
            for (int i = 0; i < pairs.length; i += 2) {
                int count = pairs[i];
                int multiplier = pairs[i + 1];
                if (count < 1) {
                    throw new IllegalArgumentException("of-a-kind count must be >= 1");
                }
                if (multiplier < 0) {
                    throw new IllegalArgumentException("pay multiplier must be >= 0");
                }
                row.put(count, multiplier);
            }
        }
    }
}
