package com.slotengine.engine.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.slotengine.engine.catalog.GameTemplateId;
import com.slotengine.engine.catalog.GameTemplates;
import com.slotengine.model.BetConfig;
import com.slotengine.model.EvaluationMode;
import com.slotengine.model.GameBuilder;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.PayUnit;
import com.slotengine.model.Payline;
import com.slotengine.model.Paylines;
import com.slotengine.model.ReelSet;
import com.slotengine.model.ReelStrip;
import com.slotengine.model.Symbol;
import com.slotengine.model.SymbolKind;
import com.slotengine.model.VolatilityClass;
import com.slotengine.model.feature.FeatureSet;
import com.slotengine.model.feature.FreeSpinsFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class JsonGameLoader {

    private final ObjectMapper mapper;

    public JsonGameLoader() {
        this.mapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public GameDefinition load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in);
        }
    }

    public GameDefinition load(InputStream in) throws IOException {
        return fromDocument(readDocument(in));
    }

    public GameDefinition load(Reader reader) throws IOException {
        return fromDocument(mapper.readValue(reader, GameDefinitionDocument.class));
    }

    public GameDefinitionDocument readDocument(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return readDocument(in);
        }
    }

    public GameDefinitionDocument readDocument(InputStream in) throws IOException {
        return mapper.readValue(in, GameDefinitionDocument.class);
    }

    public GameDocumentOverlay overlay() {
        return new GameDocumentOverlay(this);
    }

    public GameDefinition loadResource(String resourcePath) throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Game resource not found: " + resourcePath);
            }
            return load(in);
        }
    }

    public void write(GameDefinition game, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (OutputStream out = Files.newOutputStream(path)) {
            write(game, out);
        }
    }

    public void write(GameDefinition game, OutputStream out) throws IOException {
        mapper.writeValue(out, toDocument(game));
    }

    public String toJson(GameDefinition game) throws IOException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toDocument(game));
    }

    public GameDefinition fromJson(String json) throws IOException {
        return fromDocument(mapper.readValue(json, GameDefinitionDocument.class));
    }

    public GameDefinition fromDocument(GameDefinitionDocument doc) {
        if (doc.id == null || doc.id.isBlank()) {
            throw new IllegalArgumentException("game document requires id");
        }
        GameBuilder builder = startBuilder(doc);
        if (doc.symbols != null) {
            for (GameDefinitionDocument.SymbolDoc s : doc.symbols) {
                builder.symbol(toSymbol(s));
            }
        }
        if (doc.paytable != null) {
            if (doc.paytable.lineUnit != null) {
                builder.linePayUnit(PayUnit.valueOf(doc.paytable.lineUnit));
            }
            if (doc.paytable.line != null) {
                doc.paytable.line.forEach((sid, row) -> builder.linePay(sid, flatten(row)));
            }
            if (doc.paytable.scatter != null) {
                doc.paytable.scatter.forEach((sid, row) -> builder.scatterPay(sid, flatten(row)));
            }
        }
        if (doc.paylines != null && !doc.paylines.isEmpty()) {
            List<Payline> lines = new ArrayList<>();
            for (int i = 0; i < doc.paylines.size(); i++) {
                lines.add(Payline.of(i, doc.paylines.get(i)));
            }
            builder.paylines(lines);
        } else if (doc.paylinesPreset != null) {
            builder.paylines(preset(doc.paylinesPreset));
        }
        applyReels(builder, doc);
        if (doc.bothWays) {
            builder.bothWays();
        }
        if (doc.features != null) {
            applyFeatures(builder, doc.features);
        }
        if (doc.core != null) {
            applyCore(builder, doc.core);
        }
        if (doc.bets != null) {
            builder.bets(new BetConfig(
                    doc.bets.coinValues,
                    doc.bets.defaultCoinValue,
                    doc.bets.defaultCoinsPerLine,
                    doc.bets.minTotalBet,
                    doc.bets.maxTotalBet
            ));
        }
        if (doc.math != null) {
            if (doc.math.targetRtp > 0) {
                builder.targetRtp(doc.math.targetRtp);
            }
            if (doc.math.volatility != null && !doc.math.volatility.isBlank()) {
                builder.volatility(VolatilityClass.valueOf(doc.math.volatility));
            }
            if (doc.math.maxWinMultiplier > 0) {
                builder.maxWin(doc.math.maxWinMultiplier);
            }
        }
        return builder.build();
    }

    private GameBuilder startBuilder(GameDefinitionDocument doc) {
        if (doc.template != null && !doc.template.isBlank()) {
            GameBuilder builder = GameTemplates.start(doc.id, GameTemplateId.parse(doc.template));
            if (doc.name != null) {
                builder.named(doc.name);
            }
            if (doc.version != null) {
                builder.version(doc.version);
            }
            if (doc.theme != null) {
                builder.theme(doc.theme);
            }
            if (doc.grid != null) {
                builder.grid(doc.grid.reels, doc.grid.rows);
            }
            if (doc.evaluation != null) {
                builder.evaluation(EvaluationMode.valueOf(doc.evaluation));
            }
            return builder;
        }
        GameBuilder builder = GameDefinition.builder(doc.id)
                .named(doc.name == null ? doc.id : doc.name)
                .version(doc.version == null ? "1.0.0" : doc.version)
                .theme(doc.theme == null ? "" : doc.theme);
        if (doc.grid != null) {
            builder.grid(doc.grid.reels, doc.grid.rows);
        } else {
            builder.grid(5, 3);
        }
        if (doc.evaluation != null) {
            builder.evaluation(EvaluationMode.valueOf(doc.evaluation));
        }
        return builder;
    }

    private void applyReels(GameBuilder builder, GameDefinitionDocument doc) {
        boolean templated = doc.template != null && !doc.template.isBlank();
        if (doc.reels == null) {
            if (!templated) {
                throw new IllegalArgumentException("game document requires reels.base or reels.baseWeights (or a template)");
            }
            return;
        }
        if (doc.reels.baseWeights != null) {
            builder.baseReels(b -> doc.reels.baseWeights.forEach(b::reelWeights));
        } else if (doc.reels.base != null) {
            builder.baseReels(b -> doc.reels.base.forEach(reel -> b.reel(ReelStrip.of(reel.toArray(String[]::new)))));
        } else if (!templated) {
            throw new IllegalArgumentException("game document requires reels.base or reels.baseWeights");
        }
        if (doc.reels.freeWeights != null) {
            builder.freeReels(b -> doc.reels.freeWeights.forEach(b::reelWeights));
        } else if (doc.reels.free != null) {
            builder.freeReels(b -> doc.reels.free.forEach(reel -> b.reel(ReelStrip.of(reel.toArray(String[]::new)))));
        }
        if (doc.reels.anteWeights != null) {
            builder.anteReels(b -> doc.reels.anteWeights.forEach(b::reelWeights));
        } else if (doc.reels.ante != null) {
            builder.anteReels(b -> doc.reels.ante.forEach(reel -> b.reel(ReelStrip.of(reel.toArray(String[]::new)))));
        }
    }

    private void applyCore(GameBuilder builder, GameDefinitionDocument.CoreDoc core) {
        if (core.evaluation != null) {
            builder.evaluation(EvaluationMode.valueOf(core.evaluation));
        }
        if (Boolean.TRUE.equals(core.bothWays)) {
            builder.bothWays();
        }
        if (Boolean.TRUE.equals(core.expandingWilds)) {
            builder.expandingWilds();
        }
        if (core.maxWinMultiplier != null) {
            builder.maxWin(core.maxWinMultiplier);
        }
        if (core.freeSpinsMultiplier != null) {
            builder.freeSpinsMultiplier(core.freeSpinsMultiplier);
        }
        if (core.freeSpinsAwards != null) {
            core.freeSpinsAwards.forEach((k, v) -> builder.freeSpinsAward(Integer.parseInt(k), v));
        }
        if (core.retrigger != null) {
            builder.freeSpinsRetrigger(core.retrigger, core.retriggerSpins == null ? 0 : core.retriggerSpins);
        }
        if (core.stickyWilds != null) {
            builder.freeSpinsSticky(core.stickyWilds);
        }
        if (core.useFreeReels != null) {
            builder.freeSpinsUseFreeReels(core.useFreeReels);
        }
        if (core.buyBonusCostMultiplier != null) {
            int scatters = core.buyBonusScatters == null ? 3 : core.buyBonusScatters;
            builder.buyBonus(core.buyBonusCostMultiplier, scatters);
        }
        if (core.anteCostMultiplier != null) {
            builder.ante(core.anteCostMultiplier);
        }
        if (Boolean.TRUE.equals(core.cascade)) {
            int start = core.cascadeStartMultiplier == null ? 1 : core.cascadeStartMultiplier;
            int inc = core.cascadeIncrement == null ? 1 : core.cascadeIncrement;
            int max = core.cascadeMaxMultiplier == null ? 8 : core.cascadeMaxMultiplier;
            builder.cascade(new FeatureSet.CascadeFeature(true, start, inc, max));
        }
    }

    public GameDefinitionDocument toDocument(GameDefinition game) {
        GameDefinitionDocument doc = new GameDefinitionDocument();
        doc.id = game.id();
        doc.name = game.name();
        doc.version = game.version();
        doc.theme = game.theme();
        doc.grid = new GameDefinitionDocument.Grid();
        doc.grid.reels = game.grid().reels();
        doc.grid.rows = game.grid().rows();
        doc.evaluation = game.evaluation().name();
        doc.bothWays = game.features().bothWays();
        doc.symbols = game.symbols().values().stream().map(this::fromSymbol).toList();
        doc.paytable = new GameDefinitionDocument.PaytableDoc();
        doc.paytable.lineUnit = game.paytable().lineUnit().name();
        doc.paytable.scatterUnit = game.paytable().scatterUnit().name();
        doc.paytable.line = toPayMap(game.paytable().linePays());
        doc.paytable.scatter = toPayMap(game.paytable().scatterPays());
        doc.paylines = game.paylines().stream().map(Payline::rows).toList();
        doc.reels = new GameDefinitionDocument.ReelsDoc();
        doc.reels.base = strips(game.baseReels());
        doc.reels.free = game.freeReels().map(this::strips).orElse(null);
        doc.reels.ante = game.anteReels().map(this::strips).orElse(null);
        doc.features = toFeatures(game);
        doc.bets = new GameDefinitionDocument.BetsDoc();
        doc.bets.coinValues = game.bets().coinValues();
        doc.bets.defaultCoinValue = game.bets().defaultCoinValue();
        doc.bets.defaultCoinsPerLine = game.bets().defaultCoinsPerLine();
        doc.bets.minTotalBet = game.bets().minTotalBet();
        doc.bets.maxTotalBet = game.bets().maxTotalBet();
        doc.math = new GameDefinitionDocument.MathDoc();
        doc.math.targetRtp = game.math().targetRtp().doubleValue();
        doc.math.volatility = game.math().volatility().name();
        doc.math.maxWinMultiplier = game.math().maxWinMultiplier();
        doc.math.hitFrequencyPercent = game.math().hitFrequencyPercent();
        doc.math.notes = game.math().notes();
        return doc;
    }

    private void applyFeatures(GameBuilder builder, GameDefinitionDocument.FeaturesDoc features) {
        if (features.freeSpins != null) {
            GameDefinitionDocument.FreeSpinsDoc fs = features.freeSpins;
            String trigger = fs.triggerSymbol;
            if (trigger == null || trigger.isBlank()) {
                builder.freeSpins(b -> applyFreeSpins(b, fs));
            } else {
                builder.freeSpins(trigger, b -> applyFreeSpins(b, fs));
            }
        }
        if (features.buyBonus != null) {
            builder.buyBonus(features.buyBonus.costMultiplier, features.buyBonus.guaranteedScatterCount);
        }
        if (features.ante != null) {
            builder.ante(features.ante.costMultiplier);
        }
        if (features.cascade != null && features.cascade.enabled) {
            builder.cascade(new FeatureSet.CascadeFeature(
                    true,
                    features.cascade.startMultiplier,
                    features.cascade.increment,
                    features.cascade.maxMultiplier
            ));
        }
        if (features.expandingWilds) {
            builder.expandingWilds();
        }
        if (features.jackpot != null && features.jackpot.enabled) {
            List<FeatureSet.JackpotTier> tiers = new ArrayList<>();
            if (features.jackpot.tiers != null) {
                for (GameDefinitionDocument.JackpotTierDoc t : features.jackpot.tiers) {
                    tiers.add(new FeatureSet.JackpotTier(
                            t.id, t.seedCredits, t.contributionPerMillion, t.mysteryChancePerSpin
                    ));
                }
            }
            builder.jackpot(new FeatureSet.JackpotFeature(true, tiers));
        }
    }

    private static void applyFreeSpins(
            com.slotengine.model.feature.FreeSpinsFeature.Builder b,
            GameDefinitionDocument.FreeSpinsDoc fs
    ) {
        b.minTriggerCount(fs.minTriggerCount);
        if (fs.awards != null) {
            fs.awards.forEach((k, v) -> b.award(Integer.parseInt(k), v));
        }
        b.retrigger(fs.retrigger, fs.retriggerSpins);
        b.multiplier(fs.multiplier);
        b.useFreeReels(fs.useFreeReels);
        b.stickyWilds(fs.stickyWilds);
        b.maxAwardedSpins(fs.maxAwardedSpins);
    }

    private GameDefinitionDocument.FeaturesDoc toFeatures(GameDefinition game) {
        GameDefinitionDocument.FeaturesDoc features = new GameDefinitionDocument.FeaturesDoc();
        features.expandingWilds = game.features().expandingWilds()
                .map(FeatureSet.ExpandingWildsFeature::enabled).orElse(false);
        game.features().freeSpins().ifPresent(fs -> features.freeSpins = fromFreeSpins(fs));
        game.features().buyBonus().ifPresent(bb -> {
            features.buyBonus = new GameDefinitionDocument.BuyBonusDoc();
            features.buyBonus.costMultiplier = bb.costMultiplier();
            features.buyBonus.guaranteedScatterCount = bb.guaranteedScatterCount();
        });
        game.features().ante().ifPresent(a -> {
            features.ante = new GameDefinitionDocument.AnteDoc();
            features.ante.costMultiplier = a.costMultiplier();
        });
        game.features().cascade().ifPresent(c -> {
            features.cascade = new GameDefinitionDocument.CascadeDoc();
            features.cascade.enabled = c.enabled();
            features.cascade.startMultiplier = c.startMultiplier();
            features.cascade.increment = c.increment();
            features.cascade.maxMultiplier = c.maxMultiplier();
        });
        game.features().jackpot().ifPresent(j -> {
            features.jackpot = new GameDefinitionDocument.JackpotDoc();
            features.jackpot.enabled = j.enabled();
            features.jackpot.tiers = j.tiers().stream().map(t -> {
                GameDefinitionDocument.JackpotTierDoc d = new GameDefinitionDocument.JackpotTierDoc();
                d.id = t.id();
                d.seedCredits = t.seedCredits();
                d.contributionPerMillion = t.contributionPerMillion();
                d.mysteryChancePerSpin = t.mysteryChancePerSpin();
                return d;
            }).toList();
        });
        return features;
    }

    private static GameDefinitionDocument.FreeSpinsDoc fromFreeSpins(FreeSpinsFeature fs) {
        GameDefinitionDocument.FreeSpinsDoc d = new GameDefinitionDocument.FreeSpinsDoc();
        d.triggerSymbol = fs.triggerSymbol();
        d.minTriggerCount = fs.minTriggerCount();
        d.awards = new LinkedHashMap<>();
        fs.awards().forEach((k, v) -> d.awards.put(Integer.toString(k), v));
        d.retrigger = fs.retriggerEnabled();
        d.retriggerSpins = fs.retriggerSpins();
        d.multiplier = fs.multiplier();
        d.useFreeReels = fs.useFreeReels();
        d.stickyWilds = fs.stickyWilds();
        d.maxAwardedSpins = fs.maxAwardedSpins();
        return d;
    }

    private Symbol toSymbol(GameDefinitionDocument.SymbolDoc doc) {
        SymbolKind kind = SymbolKind.valueOf(doc.kind);
        return new Symbol(
                doc.id,
                doc.displayName == null ? doc.id : doc.displayName,
                kind,
                doc.substitutes == null ? java.util.Set.of() : java.util.Set.copyOf(doc.substitutes),
                doc.cannotReplace == null ? java.util.Set.of() : java.util.Set.copyOf(doc.cannotReplace),
                doc.wildMultiplier,
                doc.tier
        );
    }

    private GameDefinitionDocument.SymbolDoc fromSymbol(Symbol symbol) {
        GameDefinitionDocument.SymbolDoc d = new GameDefinitionDocument.SymbolDoc();
        d.id = symbol.id();
        d.displayName = symbol.displayName();
        d.kind = symbol.kind().name();
        d.wildMultiplier = symbol.wildMultiplier();
        d.tier = symbol.tier();
        d.substitutes = List.copyOf(symbol.substitutes());
        d.cannotReplace = List.copyOf(symbol.cannotReplace());
        return d;
    }

    private List<List<String>> strips(ReelSet set) {
        List<List<String>> out = new ArrayList<>();
        for (ReelStrip strip : set.reels()) {
            out.add(strip.symbols());
        }
        return out;
    }



    private static Map<String, Map<String, Integer>> toPayMap(
            Map<String, java.util.NavigableMap<Integer, Integer>> source
    ) {
        Map<String, Map<String, Integer>> out = new LinkedHashMap<>();
        source.forEach((id, row) -> {
            Map<String, Integer> mapped = new TreeMap<>();
            row.forEach((k, v) -> mapped.put(Integer.toString(k), v));
            out.put(id, mapped);
        });
        return out;
    }

    private static int[] flatten(Map<String, Integer> row) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(row.entrySet());
        int[] pairs = new int[entries.size() * 2];
        int i = 0;
        for (Map.Entry<String, Integer> e : entries) {
            pairs[i++] = Integer.parseInt(e.getKey());
            pairs[i++] = e.getValue();
        }
        return pairs;
    }

    private static List<Payline> preset(String name) {
        return switch (name) {
            case "standard10" -> Paylines.standard10();
            case "standard20" -> Paylines.standard20();
            case "standard25" -> Paylines.standard25();
            default -> throw new IllegalArgumentException("Unknown paylines preset: " + name);
        };
    }
}
