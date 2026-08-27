package com.slotengine.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A payline is a row index per reel. Length must equal the grid reel count.
 */
public record Payline(int index, String name, int[] rows) {

    public Payline {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(rows, "rows");
        if (index < 0) {
            throw new IllegalArgumentException("payline index must be >= 0");
        }
        if (rows.length == 0) {
            throw new IllegalArgumentException("payline must cover at least one reel");
        }
        rows = rows.clone();
        for (int row : rows) {
            if (row < 0) {
                throw new IllegalArgumentException("payline row must be >= 0");
            }
        }
    }

    public static Payline of(int index, int... rows) {
        return new Payline(index, "L" + (index + 1), rows);
    }

    public int reelCount() {
        return rows.length;
    }

    public int rowAt(int reel) {
        return rows[reel];
    }

    public List<Position> positions() {
        Position[] pos = new Position[rows.length];
        for (int reel = 0; reel < rows.length; reel++) {
            pos[reel] = new Position(reel, rows[reel]);
        }
        return List.of(pos);
    }

    @Override
    public int[] rows() {
        return rows.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Payline payline)) {
            return false;
        }
        return index == payline.index && Arrays.equals(rows, payline.rows);
    }

    @Override
    public int hashCode() {
        return 31 * index + Arrays.hashCode(rows);
    }
}
