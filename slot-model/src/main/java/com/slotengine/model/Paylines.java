package com.slotengine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Industry-standard payline presets used by video slots (Novomatic/IGT-style numbering).
 */
public final class Paylines {

    private Paylines() {
    }

    public static List<Payline> horizontal(int reels, int rows) {
        List<Payline> lines = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            int[] pattern = new int[reels];
            java.util.Arrays.fill(pattern, row);
            lines.add(Payline.of(row, pattern));
        }
        return List.copyOf(lines);
    }

    /** 5-reel, 3-row, 10 lines. */
    public static List<Payline> standard10() {
        return standard20().subList(0, 10);
    }

    /**
     * Classic 5×3 / 20-line map. Row 0 is top, 1 middle, 2 bottom.
     */
    public static List<Payline> standard20() {
        return List.of(
                Payline.of(0, 1, 1, 1, 1, 1),
                Payline.of(1, 0, 0, 0, 0, 0),
                Payline.of(2, 2, 2, 2, 2, 2),
                Payline.of(3, 0, 1, 2, 1, 0),
                Payline.of(4, 2, 1, 0, 1, 2),
                Payline.of(5, 0, 0, 1, 2, 2),
                Payline.of(6, 2, 2, 1, 0, 0),
                Payline.of(7, 1, 0, 0, 0, 1),
                Payline.of(8, 1, 2, 2, 2, 1),
                Payline.of(9, 0, 1, 0, 1, 0),
                Payline.of(10, 2, 1, 2, 1, 2),
                Payline.of(11, 1, 0, 1, 0, 1),
                Payline.of(12, 1, 2, 1, 2, 1),
                Payline.of(13, 0, 1, 1, 1, 0),
                Payline.of(14, 2, 1, 1, 1, 2),
                Payline.of(15, 1, 1, 0, 1, 1),
                Payline.of(16, 1, 1, 2, 1, 1),
                Payline.of(17, 0, 0, 2, 0, 0),
                Payline.of(18, 2, 2, 0, 2, 2),
                Payline.of(19, 0, 2, 0, 2, 0)
        );
    }

    public static List<Payline> standard25() {
        List<Payline> lines = new ArrayList<>(standard20());
        lines.add(Payline.of(20, 0, 2, 2, 2, 0));
        lines.add(Payline.of(21, 2, 0, 0, 0, 2));
        lines.add(Payline.of(22, 1, 0, 2, 0, 1));
        lines.add(Payline.of(23, 1, 2, 0, 2, 1));
        lines.add(Payline.of(24, 0, 1, 2, 2, 1));
        return List.copyOf(lines);
    }
}
