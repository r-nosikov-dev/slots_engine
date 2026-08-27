package com.slotengine.model;

import com.slotengine.model.feature.FeatureSet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable compiled game. This is the artefact the engine produces and the
 * engine/math modules consume. Safe to share across threads.
 */
public final class GameDefinition {

    private final String id;
    private final String name;
    private final String version;
    private final String theme;
    private final GridSize grid;
    private final EvaluationMode evaluation;
    private final Map<String, Symbol> symbols;
    private final Paytable paytable;
    private final List<Payline> paylines;
    private final ReelSet baseReels;
    private final Optional<ReelSet> freeReels;
    private final Optional<ReelSet> anteReels;
    private final FeatureSet features;
    private final BetConfig bets;
    private final MathSpec math;

    GameDefinition(
            String id,
            String name,
            String version,
            String theme,
            GridSize grid,
            EvaluationMode evaluation,
            Map<String, Symbol> symbols,
            Paytable paytable,
            List<Payline> paylines,
            ReelSet baseReels,
            Optional<ReelSet> freeReels,
            Optional<ReelSet> anteReels,
            FeatureSet features,
            BetConfig bets,
            MathSpec math
    ) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.theme = theme;
        this.grid = grid;
        this.evaluation = evaluation;
        this.symbols = symbols;
        this.paytable = paytable;
        this.paylines = paylines;
        this.baseReels = baseReels;
        this.freeReels = freeReels;
        this.anteReels = anteReels;
        this.features = features;
        this.bets = bets;
        this.math = math;
    }

    public static GameBuilder builder(String id) {
        return new GameBuilder(id);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String version() {
        return version;
    }

    public String theme() {
        return theme;
    }

    public GridSize grid() {
        return grid;
    }

    public EvaluationMode evaluation() {
        return evaluation;
    }

    public Map<String, Symbol> symbols() {
        return symbols;
    }

    public Symbol symbol(String symbolId) {
        Symbol symbol = symbols.get(symbolId);
        if (symbol == null) {
            throw new IllegalArgumentException("Unknown symbol '" + symbolId + "' in game " + id);
        }
        return symbol;
    }

    public Optional<Symbol> findSymbol(String symbolId) {
        return Optional.ofNullable(symbols.get(symbolId));
    }

    public List<Symbol> wilds() {
        return symbols.values().stream().filter(Symbol::isWild).toList();
    }

    public List<Symbol> scatters() {
        return symbols.values().stream().filter(Symbol::isScatter).toList();
    }

    public Paytable paytable() {
        return paytable;
    }

    public List<Payline> paylines() {
        return paylines;
    }

    public int lineCount() {
        return paylines.size();
    }

    public ReelSet baseReels() {
        return baseReels;
    }

    public Optional<ReelSet> freeReels() {
        return freeReels;
    }

    public Optional<ReelSet> anteReels() {
        return anteReels;
    }

    public FeatureSet features() {
        return features;
    }

    public BetConfig bets() {
        return bets;
    }

    public MathSpec math() {
        return math;
    }

    public ReelSet reelsFor(PlayMode mode, boolean inFreeSpins) {
        if (inFreeSpins && freeReels.isPresent()) {
            return freeReels.get();
        }
        if (mode == PlayMode.ANTE && anteReels.isPresent()) {
            return anteReels.get();
        }
        return baseReels;
    }

    /**
     * Stake per payline: {@code totalBet / lineCount} for payline games,
     * {@code totalBet} for ways (pays are already in total-bet units).
     */
    public long lineBet(long totalBet) {
        if (evaluation == EvaluationMode.WAYS || paylines.isEmpty()) {
            return totalBet;
        }
        return totalBet / paylines.size();
    }

    public void assertBetLegal(long totalBet) {
        if (totalBet < bets.minTotalBet() || totalBet > bets.maxTotalBet()) {
            throw new IllegalArgumentException(
                    "totalBet " + totalBet + " outside [" + bets.minTotalBet() + ", " + bets.maxTotalBet() + "]"
            );
        }
        if (evaluation == EvaluationMode.PAYLINES && !paylines.isEmpty() && totalBet % paylines.size() != 0) {
            throw new IllegalArgumentException(
                    "totalBet " + totalBet + " must be divisible by line count " + paylines.size()
            );
        }
    }

    public Set<String> regularSymbolIds() {
        return symbols.values().stream()
                .filter(s -> s.kind() == SymbolKind.REGULAR)
                .map(Symbol::id)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    public Map<String, Object> summary() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("version", version);
        map.put("grid", grid.toString());
        map.put("evaluation", evaluation.name());
        map.put("symbols", symbols.size());
        map.put("paylines", paylines.size());
        map.put("ways", evaluation == EvaluationMode.WAYS ? grid.ways() : 0);
        map.put("targetRtp", math.targetRtp());
        map.put("volatility", math.volatility().name());
        map.put("maxWin", math.maxWinMultiplier() + "x");
        map.put("freeSpins", features.freeSpins().isPresent());
        map.put("buyBonus", features.buyBonus().isPresent());
        map.put("cascade", features.cascade().map(FeatureSet.CascadeFeature::enabled).orElse(false));
        return map;
    }
}
