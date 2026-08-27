package com.slotengine.model;

/** Visible cell: reel is 0-based left-to-right, row is 0-based top-to-bottom. */
public record Position(int reel, int row) {

    public Position {
        if (reel < 0 || row < 0) {
            throw new IllegalArgumentException("position must be non-negative: " + reel + "," + row);
        }
    }
}
