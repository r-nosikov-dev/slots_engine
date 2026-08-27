package com.slotengine.engine.eval;

import com.slotengine.model.GameDefinition;
import com.slotengine.model.Position;
import com.slotengine.model.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Scatter pays anywhere in the window (not on a payline).
 * Amount: {@code scatterPay[symbol][count] × totalBet}. Count is cells, not reels.
 */
public final class ScatterEvaluator {

    public List<Win> evaluate(GameDefinition game, Window window, long totalBet) {
        List<Win> wins = new ArrayList<>();
        for (Symbol scatter : game.scatters()) {
            List<Position> positions = window.positionsOf(scatter.id());
            int count = positions.size();
            int pay = game.paytable().scatterPay(scatter.id(), count);
            if (pay > 0 && count > 0) {
                long amount = (long) pay * totalBet;
                wins.add(Win.scatter(scatter.id(), count, amount, positions));
            }
        }
        return wins;
    }
}
