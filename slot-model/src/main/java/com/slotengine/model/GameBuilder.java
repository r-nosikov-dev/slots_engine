package com.slotengine.model;

import com.slotengine.model.feature.FeatureSet;
import com.slotengine.model.feature.FreeSpinsFeature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Fluent builder for a {@link GameDefinition}. Call {@link #build()} to validate
 * and freeze the game. This is the primary authoring API for math designers.
 */
public final class GameBuilder {

    private final String id;
    private String name;
    private String version = "1.0.0";
    private String theme = "";
    private GridSize grid = new GridSize(5, 3);
    private EvaluationMode evaluation = EvaluationMode.PAYLINES;
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();
    private final Paytable.Builder paytable = Paytable.builder();
    private List<Payline> paylines = List.of();
    private ReelSet baseReels;
    private ReelSet freeReels;
    private ReelSet anteReels;
    private final FeatureSet.Builder features = FeatureSet.builder();
    private FreeSpinsFeature.Builder freeSpins;
    private BetConfig bets = BetConfig.credits(1, 2, 5, 10, 25, 50, 100);
    private MathSpec math = MathSpec.of(0.9600, VolatilityClass.MEDIUM, 5000);

    GameBuilder(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("game id is required");
        }
        this.id = id;
        this.name = id;
    }

    public GameBuilder named(String name) {
        this.name = Objects.requireNonNull(name);
        return this;
    }

    public GameBuilder version(String version) {
        this.version = Objects.requireNonNull(version);
        return this;
    }

    public GameBuilder theme(String theme) {
        this.theme = theme == null ? "" : theme;
        return this;
    }

    public GameBuilder grid(int reels, int rows) {
        this.grid = new GridSize(reels, rows);
        return this;
    }

    public GameBuilder evaluation(EvaluationMode mode) {
        this.evaluation = Objects.requireNonNull(mode);
        return this;
    }

    public GameBuilder ways() {
        this.evaluation = EvaluationMode.WAYS;
        return this;
    }

    public GameBuilder symbol(Symbol symbol) {
        symbols.put(symbol.id(), symbol);
        return this;
    }

    public GameBuilder symbol(String id) {
        return symbol(Symbol.regular(id));
    }

    public GameBuilder high(String id) {
        return symbol(Symbol.regular(id, id, 3));
    }

    public GameBuilder low(String id) {
        return symbol(Symbol.regular(id, id, 1));
    }

    public GameBuilder wild(String id) {
        return symbol(Symbol.wild(id).withCannotReplace(java.util.Set.of()));
    }

    public GameBuilder wild(String id, int multiplier) {
        return symbol(Symbol.wild(id, multiplier));
    }

    public GameBuilder scatter(String id) {
        return symbol(Symbol.scatter(id));
    }

    public GameBuilder paylines(List<Payline> lines) {
        this.paylines = List.copyOf(lines);
        return this;
    }

    public GameBuilder standard20Paylines() {
        return paylines(Paylines.standard20());
    }

    public GameBuilder standard10Paylines() {
        return paylines(Paylines.standard10());
    }

    public GameBuilder linePay(String symbolId, int... countThenMultiplier) {
        paytable.line(symbolId, countThenMultiplier);
        return this;
    }

    public GameBuilder scatterPay(String symbolId, int... countThenMultiplier) {
        paytable.scatter(symbolId, countThenMultiplier);
        return this;
    }

    public GameBuilder linePayUnit(PayUnit unit) {
        paytable.lineUnit(unit);
        return this;
    }

    public GameBuilder baseReels(ReelSet reels) {
        this.baseReels = reels;
        return this;
    }

    public GameBuilder baseReels(Consumer<ReelSet.Builder> consumer) {
        ReelSet.Builder builder = ReelSet.builder("base");
        consumer.accept(builder);
        this.baseReels = builder.build();
        return this;
    }

    public GameBuilder freeReels(Consumer<ReelSet.Builder> consumer) {
        ReelSet.Builder builder = ReelSet.builder("free");
        consumer.accept(builder);
        this.freeReels = builder.build();
        return this;
    }

    public GameBuilder anteReels(Consumer<ReelSet.Builder> consumer) {
        ReelSet.Builder builder = ReelSet.builder("ante");
        consumer.accept(builder);
        this.anteReels = builder.build();
        return this;
    }

    public GameBuilder freeSpins(Consumer<FreeSpinsFeature.Builder> consumer) {
        String trigger = symbols.values().stream()
                .filter(Symbol::isScatter)
                .map(Symbol::id)
                .findFirst()
                .orElse("SCATTER");
        return freeSpins(trigger, consumer);
    }

    public GameBuilder freeSpins(String triggerSymbol, Consumer<FreeSpinsFeature.Builder> consumer) {
        FreeSpinsFeature.Builder builder = FreeSpinsFeature.builder(triggerSymbol);
        consumer.accept(builder);
        this.freeSpins = builder;
        return this;
    }

    public GameBuilder freeSpinsMultiplier(int multiplier) {
        ensureFreeSpins().multiplier(multiplier);
        return this;
    }

    public GameBuilder freeSpinsAward(int scatterCount, int spins) {
        ensureFreeSpins().award(scatterCount, spins);
        return this;
    }

    public GameBuilder freeSpinsRetrigger(boolean enabled, int spins) {
        ensureFreeSpins().retrigger(enabled, spins);
        return this;
    }

    public GameBuilder freeSpinsSticky(boolean sticky) {
        ensureFreeSpins().stickyWilds(sticky);
        return this;
    }

    public GameBuilder freeSpinsUseFreeReels(boolean use) {
        ensureFreeSpins().useFreeReels(use);
        return this;
    }

    private FreeSpinsFeature.Builder ensureFreeSpins() {
        if (freeSpins == null) {
            String trigger = symbols.values().stream()
                    .filter(Symbol::isScatter)
                    .map(Symbol::id)
                    .findFirst()
                    .orElse("SCATTER");
            freeSpins = FreeSpinsFeature.builder(trigger);
        }
        return freeSpins;
    }

    public GameBuilder buyBonus(long costMultiplier, int guaranteedScatters) {
        features.buyBonus(costMultiplier, guaranteedScatters);
        return this;
    }

    public GameBuilder ante(double costMultiplier) {
        features.ante(costMultiplier);
        return this;
    }

    public GameBuilder cascade() {
        features.cascade(FeatureSet.CascadeFeature.increasing());
        return this;
    }

    public GameBuilder cascade(FeatureSet.CascadeFeature cascade) {
        features.cascade(cascade);
        return this;
    }

    public GameBuilder expandingWilds() {
        features.expandingWilds(true);
        return this;
    }

    public GameBuilder bothWays() {
        features.bothWays(true);
        return this;
    }

    public GameBuilder jackpot(FeatureSet.JackpotFeature jackpot) {
        features.jackpot(jackpot);
        return this;
    }

    public GameBuilder bets(BetConfig bets) {
        this.bets = bets;
        return this;
    }

    public GameBuilder math(MathSpec math) {
        this.math = math;
        return this;
    }

    public GameBuilder targetRtp(double rtp) {
        this.math = new MathSpec(
                BigDecimal.valueOf(rtp),
                math.volatility(),
                math.maxWinMultiplier(),
                math.hitFrequencyPercent(),
                math.notes()
        );
        return this;
    }

    public GameBuilder volatility(VolatilityClass volatility) {
        this.math = new MathSpec(
                math.targetRtp(),
                volatility,
                math.maxWinMultiplier(),
                math.hitFrequencyPercent(),
                math.notes()
        );
        return this;
    }

    public GameBuilder maxWin(int multiplier) {
        this.math = new MathSpec(
                math.targetRtp(),
                math.volatility(),
                multiplier,
                math.hitFrequencyPercent(),
                math.notes()
        );
        return this;
    }

    public GameDefinition build() {
        if (freeSpins != null) {
            features.freeSpins(freeSpins.build());
        }
        FeatureSet featureSet = features.build();
        Paytable table = paytable.build();
        Map<String, Symbol> frozenSymbols = Map.copyOf(symbols);
        List<Payline> frozenLines = evaluation == EvaluationMode.PAYLINES
                ? List.copyOf(paylines)
                : List.copyOf(paylines);

        List<String> errors = validate(
                frozenSymbols, table, frozenLines, baseReels, freeReels, anteReels, featureSet
        );
        if (!errors.isEmpty()) {
            throw new GameValidationException(errors);
        }

        // Wilds never substitute scatters/bonus/jackpot.
        Map<String, Symbol> adjusted = new LinkedHashMap<>();
        java.util.Set<String> specials = frozenSymbols.values().stream()
                .filter(Symbol::isSpecial)
                .map(Symbol::id)
                .collect(java.util.stream.Collectors.toSet());
        frozenSymbols.forEach((sid, symbol) -> {
            if (symbol.isWild() && !specials.isEmpty()) {
                adjusted.put(sid, symbol.withCannotReplace(specials));
            } else {
                adjusted.put(sid, symbol);
            }
        });

        return new GameDefinition(
                id,
                name,
                version,
                theme,
                grid,
                evaluation,
                Map.copyOf(adjusted),
                table,
                frozenLines,
                baseReels,
                Optional.ofNullable(freeReels),
                Optional.ofNullable(anteReels),
                featureSet,
                bets,
                math
        );
    }

    private List<String> validate(
            Map<String, Symbol> frozenSymbols,
            Paytable table,
            List<Payline> frozenLines,
            ReelSet base,
            ReelSet free,
            ReelSet ante,
            FeatureSet featureSet
    ) {
        List<String> errors = new ArrayList<>();
        if (frozenSymbols.isEmpty()) {
            errors.add("at least one symbol is required");
        }
        if (base == null) {
            errors.add("base reel set is required");
        } else {
            validateReelSet("base", base, frozenSymbols, errors);
            if (base.reelCount() != grid.reels()) {
                errors.add("base reels=" + base.reelCount() + " but grid reels=" + grid.reels());
            }
        }
        if (free != null) {
            validateReelSet("free", free, frozenSymbols, errors);
            if (free.reelCount() != grid.reels()) {
                errors.add("free reels=" + free.reelCount() + " but grid reels=" + grid.reels());
            }
        }
        if (ante != null) {
            validateReelSet("ante", ante, frozenSymbols, errors);
        }
        if (evaluation == EvaluationMode.PAYLINES) {
            if (frozenLines.isEmpty()) {
                errors.add("payline evaluation requires at least one payline");
            }
            for (Payline line : frozenLines) {
                if (line.reelCount() != grid.reels()) {
                    errors.add("payline " + line.index() + " length " + line.reelCount()
                            + " != grid reels " + grid.reels());
                }
                for (int row : line.rows()) {
                    if (row >= grid.rows()) {
                        errors.add("payline " + line.index() + " row " + row + " >= grid rows " + grid.rows());
                    }
                }
            }
        }
        table.linePays().keySet().forEach(sid -> {
            if (!frozenSymbols.containsKey(sid)) {
                errors.add("paytable references unknown symbol " + sid);
            }
        });
        table.scatterPays().keySet().forEach(sid -> {
            if (!frozenSymbols.containsKey(sid)) {
                errors.add("scatter paytable references unknown symbol " + sid);
            }
        });
        featureSet.freeSpins().ifPresent(fs -> {
            if (!frozenSymbols.containsKey(fs.triggerSymbol())) {
                errors.add("free spins trigger symbol " + fs.triggerSymbol() + " is not defined");
            }
            if (fs.useFreeReels() && free == null) {
                errors.add("free spins request special reels but free reel set is missing");
            }
        });
        featureSet.buyBonus().ifPresent(bb -> {
            if (featureSet.freeSpins().isEmpty()) {
                errors.add("buy bonus requires a free-spins feature");
            }
        });
        if (table.linePays().isEmpty() && table.scatterPays().isEmpty()) {
            errors.add("paytable is empty");
        }
        return errors;
    }

    private void validateReelSet(
            String label,
            ReelSet set,
            Map<String, Symbol> frozenSymbols,
            List<String> errors
    ) {
        for (int i = 0; i < set.reelCount(); i++) {
            ReelStrip strip = set.reel(i);
            if (strip.length() < grid.rows()) {
                errors.add(label + " reel " + i + " shorter than visible rows");
            }
            for (String sid : strip.symbols()) {
                if (!frozenSymbols.containsKey(sid)) {
                    errors.add(label + " reel " + i + " contains unknown symbol " + sid);
                }
            }
        }
    }
}
