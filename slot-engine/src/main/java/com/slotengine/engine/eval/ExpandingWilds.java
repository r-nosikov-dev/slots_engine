package com.slotengine.engine.eval;

import com.slotengine.model.GameDefinition;
import com.slotengine.model.Symbol;

/**
 * If any cell on a reel is a wild, the entire reel becomes that wild before evaluation.
 */
public final class ExpandingWilds {

    public Window apply(GameDefinition game, Window window) {
        Window result = window;
        for (int reel = 0; reel < window.reels(); reel++) {
            String wildId = null;
            for (int row = 0; row < window.rows(); row++) {
                Symbol symbol = game.symbol(window.at(reel, row));
                if (symbol.isWild()) {
                    wildId = symbol.id();
                    break;
                }
            }
            if (wildId != null) {
                String[] column = new String[window.rows()];
                java.util.Arrays.fill(column, wildId);
                result = result.withColumn(reel, column);
            }
        }
        return result;
    }
}
