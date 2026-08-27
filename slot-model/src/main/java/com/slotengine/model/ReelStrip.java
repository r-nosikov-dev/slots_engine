package com.slotengine.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Physical (or virtual) reel strip. Stop {@code s} shows symbols
 * {@code strip[(s + row) % length]} for each visible row, top to bottom.
 */
public final class ReelStrip {

    private final List<String> symbols;

    public ReelStrip(List<String> symbols) {
        Objects.requireNonNull(symbols, "symbols");
        if (symbols.size() < 3) {
            throw new IllegalArgumentException("reel strip must contain at least 3 stops");
        }
        for (String id : symbols) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("reel strip contains a blank symbol id");
            }
        }
        this.symbols = List.copyOf(symbols);
    }

    public static ReelStrip of(String... ids) {
        return new ReelStrip(List.of(ids));
    }

    /**
     * Expands a weight map into a strip by repeating each symbol {@code weight} times,
     * preserving insertion order. Useful for math prototyping; production games usually
     * author explicit strips so stacked symbols and scatter spacing stay under control.
     */
    public static ReelStrip fromWeights(Map<String, Integer> weights) {
        List<String> expanded = new ArrayList<>();
        weights.forEach((id, weight) -> {
            if (weight == null || weight < 0) {
                throw new IllegalArgumentException("weight for " + id + " must be >= 0");
            }
            for (int i = 0; i < weight; i++) {
                expanded.add(id);
            }
        });
        return new ReelStrip(expanded);
    }

    public int length() {
        return symbols.size();
    }

    public String at(int stop) {
        int len = symbols.size();
        int idx = Math.floorMod(stop, len);
        return symbols.get(idx);
    }

    /** Visible column for a stop, top-to-bottom. */
    public List<String> window(int stop, int rows) {
        List<String> column = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            column.add(at(stop + row));
        }
        return List.copyOf(column);
    }

    public List<String> symbols() {
        return symbols;
    }

    public Map<String, Integer> histogram() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : symbols) {
            counts.merge(id, 1, Integer::sum);
        }
        return counts;
    }
}
