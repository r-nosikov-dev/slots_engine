package com.slotengine.engine.eval;

import com.slotengine.model.GameDefinition;
import com.slotengine.model.PayUnit;
import com.slotengine.model.Payline;
import com.slotengine.model.Position;
import com.slotengine.model.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Left-to-right (and optionally right-to-left) payline scanner with wild substitution.
 *
 * <p>Rules:
 * <ul>
 *   <li>One win per line per direction: the longest consecutive match from that edge.</li>
 *   <li>Wilds substitute regular symbols; they never substitute scatters/bonus/jackpot.</li>
 *   <li>If the line is all wilds, the wild's own paytable is used.</li>
 *   <li>A 5-of-a-kind both-ways win is paid once (left-to-right only).</li>
 * </ul>
 */
public final class PaylineEvaluator {

    /**
     * Amount for a line hit: {@code paytable[symbol][count] × lineBet × extraMultiplier × wildProduct}.
     * {@code lineBet = totalBet / lineCount} unless the paytable unit is {@code TOTAL_BET}.
     */
    public List<Win> evaluate(GameDefinition game, Window window, long totalBet, int extraMultiplier) {
        List<Win> wins = new ArrayList<>();
        long lineBet = game.lineBet(totalBet);
        for (Payline line : game.paylines()) {
            addLineWin(game, window, line, lineBet, extraMultiplier, true, wins);
            if (game.features().bothWays() && game.grid().reels() > 1) {
                addLineWin(game, window, line, lineBet, extraMultiplier, false, wins);
            }
        }
        return wins;
    }

    private void addLineWin(
            GameDefinition game,
            Window window,
            Payline line,
            long lineBet,
            int extraMultiplier,
            boolean leftToRight,
            List<Win> wins
    ) {
        int reels = line.reelCount();
        String[] along = new String[reels];
        for (int i = 0; i < reels; i++) {
            int reel = leftToRight ? i : reels - 1 - i;
            along[i] = window.at(reel, line.rowAt(reel));
        }

        Match match = match(game, along);
        if (match == null) {
            return;
        }
        if (!leftToRight && match.count == reels) {
            return;
        }
        int pay = game.paytable().linePay(match.symbolId, match.count);
        if (pay <= 0) {
            return;
        }
        int wildMult = match.wildMultiplier;
        int multiplier = extraMultiplier * wildMult;
        long amount = pay * lineBet * multiplier;
        if (game.paytable().lineUnit() == PayUnit.TOTAL_BET) {
            amount = pay * (lineBet * game.lineCount()) * multiplier;
        }
        List<Position> positions = new ArrayList<>(match.count);
        for (int i = 0; i < match.count; i++) {
            int reel = leftToRight ? i : reels - 1 - i;
            positions.add(new Position(reel, line.rowAt(reel)));
        }
        wins.add(Win.line(match.symbolId, match.count, line.index(), amount, multiplier, positions));
    }

    /**
     * Longest left-aligned run of {@code payingId} allowing wild substitution.
     * Paying symbol is the first non-wild from the left; all-wild uses the wild id.
     */
    private Match match(GameDefinition game, String[] along) {
        String first = along[0];
        Symbol firstSymbol = game.symbol(first);
        if (firstSymbol.isSpecial()) {
            return null;
        }

        String payingId = first;
        int wildMultiplier = 1;
        if (firstSymbol.isWild()) {
            wildMultiplier = lcmOrMax(wildMultiplier, firstSymbol.wildMultiplier());
            payingId = null;
            for (int i = 1; i < along.length; i++) {
                Symbol s = game.symbol(along[i]);
                if (s.isWild()) {
                    wildMultiplier = lcmOrMax(wildMultiplier, s.wildMultiplier());
                    continue;
                }
                if (s.isSpecial()) {
                    break;
                }
                if (firstSymbol.substitutesFor(s) || game.wilds().stream().anyMatch(w -> w.substitutesFor(s))) {
                    payingId = s.id();
                    break;
                }
                break;
            }
            if (payingId == null) {
                payingId = firstSymbol.id();
            }
        }

        Symbol paying = game.symbol(payingId);
        int count = 0;
        for (String id : along) {
            Symbol s = game.symbol(id);
            if (s.id().equals(payingId)) {
                count++;
                continue;
            }
            if (s.isWild() && s.substitutesFor(paying)) {
                wildMultiplier = lcmOrMax(wildMultiplier, s.wildMultiplier());
                count++;
                continue;
            }
            break;
        }
        if (count < 1) {
            return null;
        }
        return new Match(payingId, count, wildMultiplier);
    }

    /**
     * Wild multipliers on a line stack as a product when several multiplied wilds
     * participate; a 1x wild is a no-op. Using product is the common video-slot rule.
     */
    private static int lcmOrMax(int current, int incoming) {
        return Math.multiplyExact(current, incoming);
    }

    private record Match(String symbolId, int count, int wildMultiplier) {
    }
}
