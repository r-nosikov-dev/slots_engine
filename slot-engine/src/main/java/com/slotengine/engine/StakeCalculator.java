package com.slotengine.engine;

import com.slotengine.model.GameDefinition;
import com.slotengine.model.PlayMode;

public final class StakeCalculator {

    private StakeCalculator() {
    }

    /**
     * Credits deducted for one round: {@code totalBet} (NORMAL),
     * {@code round(totalBet × anteCost)} (ANTE), {@code totalBet × buyCost} (BUY_BONUS).
     */
    public static long charged(GameDefinition game, long totalBet, PlayMode mode) {
        return switch (mode) {
            case NORMAL -> totalBet;
            case ANTE -> {
                double mult = game.features().ante()
                        .orElseThrow(() -> new IllegalArgumentException("ante is not enabled"))
                        .costMultiplier();
                yield Math.round(totalBet * mult);
            }
            case BUY_BONUS -> {
                long mult = game.features().buyBonus()
                        .orElseThrow(() -> new IllegalArgumentException("buy bonus is not enabled"))
                        .costMultiplier();
                yield Math.multiplyExact(totalBet, mult);
            }
        };
    }
}
