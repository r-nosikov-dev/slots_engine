package com.slotengine.engine;

import com.slotengine.engine.eval.CascadeProcessor;
import com.slotengine.engine.eval.ExpandingWilds;
import com.slotengine.engine.eval.PaylineEvaluator;
import com.slotengine.engine.eval.ScatterEvaluator;
import com.slotengine.engine.eval.WaysEvaluator;
import com.slotengine.engine.eval.Win;
import com.slotengine.engine.eval.Window;
import com.slotengine.engine.rng.GameRng;
import com.slotengine.engine.rng.SecureSeeds;
import com.slotengine.engine.rng.SeededGameRng;
import com.slotengine.model.EvaluationMode;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.PlayMode;
import com.slotengine.model.Position;
import com.slotengine.model.ReelSet;
import com.slotengine.model.Symbol;
import com.slotengine.model.feature.FeatureSet;
import com.slotengine.model.feature.FreeSpinsFeature;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pure math runtime. Stateless and thread-safe: every {@link #play} call is isolated.
 * Wallet, session and persistence live outside this class so the same engine can
 * drive a REST API, a simulator, or an offline studio tool.
 */
public final class SlotEngine {

    private final PaylineEvaluator paylineEvaluator = new PaylineEvaluator();
    private final WaysEvaluator waysEvaluator = new WaysEvaluator();
    private final ScatterEvaluator scatterEvaluator = new ScatterEvaluator();
    private final ExpandingWilds expandingWilds = new ExpandingWilds();
    private final CascadeProcessor cascadeProcessor = new CascadeProcessor();

    public RoundResult play(GameDefinition game, PlayRequest request) {
        game.assertBetLegal(request.totalBet());
        PlayMode mode = request.mode();
        if (mode == PlayMode.BUY_BONUS && game.features().buyBonus().isEmpty()) {
            throw new IllegalArgumentException("buy bonus is not enabled for " + game.id());
        }
        if (mode == PlayMode.ANTE && game.features().ante().isEmpty()) {
            throw new IllegalArgumentException("ante is not enabled for " + game.id());
        }

        long seed = request.seed().orElseGet(SecureSeeds::next);
        GameRng rng = new SeededGameRng(seed);
        long charged = StakeCalculator.charged(game, request.totalBet(), mode);
        // Cap is a multiple of the selected total bet, not of the charged ante/buy cost.
        long maxWin = Math.multiplyExact(request.totalBet(), (long) game.math().maxWinMultiplier());

        List<SpinSnapshot> spins = new ArrayList<>();
        List<FeatureTrigger> roundTriggers = new ArrayList<>();
        long totalWin = 0;
        boolean capped = false;
        int index = 0;

        if (mode == PlayMode.BUY_BONUS) {
            FreeSpinsFeature fs = game.features().freeSpins().orElseThrow();
            int scatters = game.features().buyBonus().orElseThrow().guaranteedScatterCount();
            int awarded = fs.spinsFor(scatters);
            FeatureTrigger trigger = FeatureTrigger.freeSpins(fs.triggerSymbol(), scatters, awarded);
            roundTriggers.add(trigger);
            spins.add(new SpinSnapshot(
                    index++,
                    SpinPhase.BUY_BONUS_ENTRY,
                    emptyWindow(game),
                    List.of(),
                    0,
                    1,
                    List.of(trigger),
                    awarded
            ));
            FsOutcome fsOutcome = resolveFreeSpins(
                    game, request, rng, spins, index, awarded, request.totalBet(), maxWin, 0, fs
            );
            totalWin = fsOutcome.totalWin();
            capped = fsOutcome.capped();
            roundTriggers.addAll(fsOutcome.triggers());
        } else {
            ReelSet reels = game.reelsFor(mode, false);
            SpinBuild base = resolveSpin(
                    game, reels, rng, request.totalBet(), 1, SpinPhase.BASE, index++, true, Set.of()
            );
            spins.addAll(base.snapshots);
            totalWin += base.winAmount;
            index = spins.size();
            roundTriggers.addAll(base.triggers);

            if (totalWin >= maxWin) {
                capped = true;
                totalWin = maxWin;
            }

            FreeSpinsFeature fs = game.features().freeSpins().orElse(null);
            if (!capped && fs != null) {
                int scatterCount = scatterCount(game, base.finalWindow, fs.triggerSymbol());
                int awarded = fs.spinsFor(scatterCount);
                if (awarded > 0) {
                    FeatureTrigger trigger = FeatureTrigger.freeSpins(fs.triggerSymbol(), scatterCount, awarded);
                    roundTriggers.add(trigger);
                    FsOutcome fsOutcome = resolveFreeSpins(
                            game, request, rng, spins, index, awarded, request.totalBet(), maxWin, totalWin, fs
                    );
                    totalWin = fsOutcome.totalWin();
                    capped = fsOutcome.capped();
                    roundTriggers.addAll(fsOutcome.triggers());
                }
            }
        }

        if (totalWin > maxWin) {
            totalWin = maxWin;
            capped = true;
        }

        String roundId = request.roundId() == null ? UUID.randomUUID().toString() : request.roundId();
        return new RoundResult(
                roundId,
                game.id(),
                mode,
                seed,
                request.totalBet(),
                charged,
                spins,
                totalWin,
                capped,
                roundTriggers
        );
    }

    private FsOutcome resolveFreeSpins(
            GameDefinition game,
            PlayRequest request,
            GameRng rng,
            List<SpinSnapshot> spins,
            int index,
            int initialAward,
            long totalBet,
            long maxWin,
            long runningWin,
            FreeSpinsFeature fs
    ) {
        int remaining = initialAward;
        int awardedTotal = initialAward;
        Set<Position> sticky = new LinkedHashSet<>();
        List<FeatureTrigger> triggers = new ArrayList<>();
        boolean capped = false;
        ReelSet reels = game.reelsFor(request.mode(), fs.useFreeReels());
        int fsMultiplier = fs.multiplier();

        while (remaining > 0 && !capped) {
            remaining--;
            SpinBuild spin = resolveSpin(
                    game, reels, rng, totalBet, fsMultiplier, SpinPhase.FREE_SPIN, index, true, sticky
            );
            index += spin.snapshots.size();
            spins.addAll(spin.snapshots);
            runningWin += spin.winAmount;
            if (fs.stickyWilds()) {
                sticky.addAll(wildPositions(game, spin.finalWindow));
            }
            int scatterCount = scatterCount(game, spin.finalWindow, fs.triggerSymbol());
            if (fs.retriggerEnabled() && scatterCount >= fs.minTriggerCount()) {
                int extra = fs.retriggerSpins();
                if (awardedTotal + extra > fs.maxAwardedSpins()) {
                    extra = Math.max(0, fs.maxAwardedSpins() - awardedTotal);
                }
                if (extra > 0) {
                    remaining += extra;
                    awardedTotal += extra;
                    FeatureTrigger retrigger = FeatureTrigger.retrigger(fs.triggerSymbol(), scatterCount, extra);
                    triggers.add(retrigger);
                    SpinSnapshot last = spins.get(spins.size() - 1);
                    List<FeatureTrigger> merged = new ArrayList<>(last.triggers());
                    merged.add(retrigger);
                    spins.set(spins.size() - 1, new SpinSnapshot(
                            last.index(), last.phase(), last.window(), last.wins(), last.winAmount(),
                            last.multiplier(), merged, remaining
                    ));
                }
            }
            if (runningWin >= maxWin) {
                runningWin = maxWin;
                capped = true;
            }
        }
        return new FsOutcome(runningWin, capped, triggers);
    }

    private SpinBuild resolveSpin(
            GameDefinition game,
            ReelSet reels,
            GameRng rng,
            long totalBet,
            int featureMultiplier,
            SpinPhase phase,
            int index,
            boolean includeScatters,
            Set<Position> stickyWilds
    ) {
        int[] stops = new int[reels.reelCount()];
        for (int i = 0; i < stops.length; i++) {
            stops[i] = rng.nextInt(reels.reel(i).length());
        }
        Window window = Window.fromStops(reels, stops, game.grid().rows());
        window = overlaySticky(game, window, stickyWilds);
        if (game.features().expandingWilds().map(FeatureSet.ExpandingWildsFeature::enabled).orElse(false)) {
            window = expandingWilds.apply(game, window);
        }

        FeatureSet.CascadeFeature cascade = game.features().cascade()
                .orElse(FeatureSet.CascadeFeature.off());
        int multiplier = featureMultiplier * (cascade.enabled() ? cascade.startMultiplier() : 1);
        int[] pulled = new int[reels.reelCount()];
        List<SpinSnapshot> snapshots = new ArrayList<>();
        List<FeatureTrigger> triggers = new ArrayList<>();
        long winAmount = 0;
        boolean first = true;
        SpinPhase currentPhase = phase;
        Window current = window;

        while (true) {
            List<Win> wins = evaluate(game, current, totalBet, multiplier, first && includeScatters);
            wins.addAll(rollJackpots(game, rng, totalBet, first));
            long stepWin = 0;
            for (Win win : wins) {
                stepWin += win.amount();
            }
            winAmount += stepWin;
            snapshots.add(new SpinSnapshot(
                    index++,
                    currentPhase,
                    current,
                    wins,
                    stepWin,
                    multiplier,
                    first ? List.copyOf(triggers) : List.of(),
                    0
            ));

            if (!cascade.enabled()) {
                break;
            }
            boolean hasDrop = wins.stream().anyMatch(w ->
                    w.type() != com.slotengine.engine.eval.WinType.SCATTER
                            && w.type() != com.slotengine.engine.eval.WinType.JACKPOT
                            && w.amount() > 0);
            if (!hasDrop) {
                break;
            }
            current = cascadeProcessor.drop(game, reels, current, wins, pulled);
            multiplier = featureMultiplier * CascadeProcessor.nextMultiplier(cascade, multiplier / Math.max(1, featureMultiplier));
            currentPhase = SpinPhase.CASCADE;
            first = false;
        }

        return new SpinBuild(snapshots, current, winAmount, triggers);
    }

    private List<Win> evaluate(
            GameDefinition game,
            Window window,
            long totalBet,
            int multiplier,
            boolean includeScatters
    ) {
        List<Win> wins = new ArrayList<>();
        if (game.evaluation() == EvaluationMode.WAYS) {
            wins.addAll(waysEvaluator.evaluate(game, window, totalBet, multiplier));
        } else {
            wins.addAll(paylineEvaluator.evaluate(game, window, totalBet, multiplier));
        }
        if (includeScatters) {
            wins.addAll(scatterEvaluator.evaluate(game, window, totalBet));
        }
        return wins;
    }

    private List<Win> rollJackpots(GameDefinition game, GameRng rng, long totalBet, boolean first) {
        if (!first) {
            return List.of();
        }
        FeatureSet.JackpotFeature jackpot = game.features().jackpot().orElse(null);
        if (jackpot == null || !jackpot.enabled()) {
            return List.of();
        }
        List<Win> wins = new ArrayList<>();
        for (FeatureSet.JackpotTier tier : jackpot.tiers()) {
            if (tier.mysteryChancePerSpin() > 0 && rng.nextDouble() < tier.mysteryChancePerSpin()) {
                long amount = tier.seedCredits() > 0 ? tier.seedCredits() : totalBet * 100;
                wins.add(Win.jackpot(tier.id(), amount));
            }
        }
        return wins;
    }

    private static int scatterCount(GameDefinition game, Window window, String symbolId) {
        if (symbolId == null) {
            return 0;
        }
        return window.count(symbolId);
    }

    private static Set<Position> wildPositions(GameDefinition game, Window window) {
        Set<Position> positions = new LinkedHashSet<>();
        for (Symbol wild : game.wilds()) {
            positions.addAll(window.positionsOf(wild.id()));
        }
        return positions;
    }

    private static Window overlaySticky(GameDefinition game, Window window, Set<Position> sticky) {
        if (sticky == null || sticky.isEmpty() || game.wilds().isEmpty()) {
            return window;
        }
        String wildId = game.wilds().get(0).id();
        Window result = window;
        for (Position p : sticky) {
            if (p.reel() < window.reels() && p.row() < window.rows()) {
                result = result.withCell(p.reel(), p.row(), wildId);
            }
        }
        return result;
    }

    private static Window emptyWindow(GameDefinition game) {
        String filler = game.symbols().keySet().iterator().next();
        String[][] cells = new String[game.grid().reels()][game.grid().rows()];
        for (int r = 0; r < game.grid().reels(); r++) {
            java.util.Arrays.fill(cells[r], filler);
        }
        return new Window(cells, new int[game.grid().reels()]);
    }

    private record SpinBuild(
            List<SpinSnapshot> snapshots,
            Window finalWindow,
            long winAmount,
            List<FeatureTrigger> triggers
    ) {
    }

    private record FsOutcome(long totalWin, boolean capped, List<FeatureTrigger> triggers) {
    }
}
