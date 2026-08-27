package com.slotengine.engine.eval;

import com.slotengine.model.GameDefinition;
import com.slotengine.model.Position;
import com.slotengine.model.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * All-ways evaluation (243 / 1024 / …). For each paying symbol, count how many
 * times it (or a substituting wild) appears on each consecutive reel from the left.
 * Ways = product of those per-reel counts.
 */
public final class WaysEvaluator {

    /**
     * Per-symbol win: {@code pay × totalBet × ways × extraMultiplier × wildProduct},
     * where {@code ways = Π counts[reel]} over the longest left-aligned run with count &gt; 0.
     */
    public List<Win> evaluate(GameDefinition game, Window window, long totalBet, int extraMultiplier) {
        List<Win> wins = new ArrayList<>();
        for (String symbolId : game.regularSymbolIds()) {
            addSymbolWin(game, window, symbolId, totalBet, extraMultiplier, wins);
        }
        for (Symbol wild : game.wilds()) {
            if (game.paytable().hasLinePays(wild.id())) {
                addPureWildWin(game, window, wild, totalBet, extraMultiplier, wins);
            }
        }
        return wins;
    }

    private void addSymbolWin(
            GameDefinition game,
            Window window,
            String symbolId,
            long totalBet,
            int extraMultiplier,
            List<Win> wins
    ) {
        Symbol target = game.symbol(symbolId);
        int reels = window.reels();
        int[] counts = new int[reels];
        int[] wildMultOnReel = new int[reels];
        List<List<Position>> positionsByReel = new ArrayList<>(reels);

        for (int reel = 0; reel < reels; reel++) {
            List<Position> pos = new ArrayList<>();
            int wildMult = 1;
            for (int row = 0; row < window.rows(); row++) {
                String id = window.at(reel, row);
                Symbol s = game.symbol(id);
                if (s.id().equals(symbolId) || (s.isWild() && s.substitutesFor(target))) {
                    pos.add(new Position(reel, row));
                    if (s.isWild()) {
                        wildMult = Math.max(wildMult, s.wildMultiplier());
                    }
                }
            }
            counts[reel] = pos.size();
            wildMultOnReel[reel] = wildMult;
            positionsByReel.add(pos);
        }

        int length = 0;
        int ways = 1;
        int wildMult = 1;
        List<Position> used = new ArrayList<>();
        // Left-aligned: stop at the first reel with zero matching symbols.
        for (int reel = 0; reel < reels; reel++) {
            if (counts[reel] == 0) {
                break;
            }
            ways *= counts[reel];
            wildMult *= wildMultOnReel[reel];
            used.addAll(positionsByReel.get(reel));
            length++;
        }
        if (length == 0) {
            return;
        }
        int pay = game.paytable().linePay(symbolId, length);
        if (pay <= 0) {
            return;
        }
        int multiplier = extraMultiplier * wildMult;
        long amount = (long) pay * totalBet * ways * multiplier;
        wins.add(Win.ways(symbolId, length, ways, amount, multiplier, used));
    }

    private void addPureWildWin(
            GameDefinition game,
            Window window,
            Symbol wild,
            long totalBet,
            int extraMultiplier,
            List<Win> wins
    ) {
        int reels = window.reels();
        int length = 0;
        int ways = 1;
        List<Position> used = new ArrayList<>();
        int wildMult = 1;
        for (int reel = 0; reel < reels; reel++) {
            List<Position> pos = new ArrayList<>();
            int reelMult = 1;
            for (int row = 0; row < window.rows(); row++) {
                if (wild.id().equals(window.at(reel, row))) {
                    pos.add(new Position(reel, row));
                    reelMult = Math.max(reelMult, wild.wildMultiplier());
                }
            }
            if (pos.isEmpty()) {
                break;
            }
            ways *= pos.size();
            wildMult *= reelMult;
            used.addAll(pos);
            length++;
        }
        int pay = game.paytable().linePay(wild.id(), length);
        if (pay <= 0 || length == 0) {
            return;
        }
        int multiplier = extraMultiplier * wildMult;
        long amount = (long) pay * totalBet * ways * multiplier;
        wins.add(Win.ways(wild.id(), length, ways, amount, multiplier, used));
    }
}
