package com.slotengine.engine.eval;

import com.slotengine.model.Position;

import java.util.List;

public record Win(
        WinType type,
        String symbolId,
        int ofAKind,
        int waysOrLine,
        long amount,
        int multiplier,
        List<Position> positions
) {

    public Win {
        positions = positions == null ? List.of() : List.copyOf(positions);
    }

    public static Win line(
            String symbolId,
            int ofAKind,
            int lineIndex,
            long amount,
            int multiplier,
            List<Position> positions
    ) {
        return new Win(WinType.LINE, symbolId, ofAKind, lineIndex, amount, multiplier, positions);
    }

    public static Win ways(
            String symbolId,
            int ofAKind,
            int ways,
            long amount,
            int multiplier,
            List<Position> positions
    ) {
        return new Win(WinType.WAYS, symbolId, ofAKind, ways, amount, multiplier, positions);
    }

    public static Win scatter(String symbolId, int count, long amount, List<Position> positions) {
        return new Win(WinType.SCATTER, symbolId, count, 1, amount, 1, positions);
    }

    public static Win jackpot(String tierId, long amount) {
        return new Win(WinType.JACKPOT, tierId, 1, 1, amount, 1, List.of());
    }
}
