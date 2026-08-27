package com.slotengine.engine.config;

import com.slotengine.model.GameDefinition;
import com.slotengine.model.ReelSet;
import com.slotengine.model.feature.FeatureSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact, editable view of a game's math and engine knobs — what a designer
 * actually tweaks between simulation runs.
 */
public final class MathSnapshot {

    public String id;
    public String name;
    public String grid;
    public String evaluation;
    public int paylines;
    public Integer ways;
    public long cycleSize;
    public Map<String, Object> math;
    public Map<String, Object> core;
    public Map<String, Map<String, Integer>> linePays;
    public Map<String, Map<String, Integer>> scatterPays;
    public List<Map<String, Integer>> baseWeights;
    public List<Map<String, Integer>> freeWeights;
    public List<Map<String, Integer>> anteWeights;

    public static MathSnapshot of(GameDefinition game) {
        MathSnapshot snap = new MathSnapshot();
        snap.id = game.id();
        snap.name = game.name();
        snap.grid = game.grid().toString();
        snap.evaluation = game.evaluation().name();
        snap.paylines = game.lineCount();
        snap.ways = game.evaluation().name().equals("WAYS") ? game.grid().ways() : null;
        snap.cycleSize = game.baseReels().cycleSize();
        snap.math = new LinkedHashMap<>();
        snap.math.put("targetRtp", game.math().targetRtp());
        snap.math.put("volatility", game.math().volatility().name());
        snap.math.put("maxWinMultiplier", game.math().maxWinMultiplier());
        snap.math.put("notes", game.math().notes());
        snap.core = core(game);
        snap.linePays = pays(game.paytable().linePays());
        snap.scatterPays = pays(game.paytable().scatterPays());
        snap.baseWeights = weights(game.baseReels());
        snap.freeWeights = game.freeReels().map(MathSnapshot::weights).orElse(null);
        snap.anteWeights = game.anteReels().map(MathSnapshot::weights).orElse(null);
        return snap;
    }

    private static Map<String, Object> core(GameDefinition game) {
        Map<String, Object> core = new LinkedHashMap<>();
        core.put("evaluation", game.evaluation().name());
        core.put("bothWays", game.features().bothWays());
        core.put("expandingWilds", game.features().expandingWilds()
                .map(FeatureSet.ExpandingWildsFeature::enabled).orElse(false));
        core.put("maxWinMultiplier", game.math().maxWinMultiplier());
        game.features().freeSpins().ifPresent(fs -> {
            core.put("freeSpinsMultiplier", fs.multiplier());
            Map<String, Integer> awards = new LinkedHashMap<>();
            fs.awards().forEach((k, v) -> awards.put(Integer.toString(k), v));
            core.put("freeSpinsAwards", awards);
            core.put("retrigger", fs.retriggerEnabled());
            core.put("retriggerSpins", fs.retriggerSpins());
            core.put("stickyWilds", fs.stickyWilds());
            core.put("useFreeReels", fs.useFreeReels());
        });
        game.features().buyBonus().ifPresent(bb -> {
            core.put("buyBonusCostMultiplier", bb.costMultiplier());
            core.put("buyBonusScatters", bb.guaranteedScatterCount());
        });
        game.features().ante().ifPresent(a -> core.put("anteCostMultiplier", a.costMultiplier()));
        game.features().cascade().ifPresent(c -> {
            core.put("cascade", c.enabled());
            core.put("cascadeStartMultiplier", c.startMultiplier());
            core.put("cascadeIncrement", c.increment());
            core.put("cascadeMaxMultiplier", c.maxMultiplier());
        });
        return core;
    }

    private static Map<String, Map<String, Integer>> pays(
            Map<String, java.util.NavigableMap<Integer, Integer>> source
    ) {
        Map<String, Map<String, Integer>> out = new LinkedHashMap<>();
        source.forEach((id, row) -> {
            Map<String, Integer> mapped = new LinkedHashMap<>();
            row.forEach((k, v) -> mapped.put(Integer.toString(k), v));
            out.put(id, mapped);
        });
        return out;
    }

    private static List<Map<String, Integer>> weights(ReelSet set) {
        List<Map<String, Integer>> out = new ArrayList<>();
        set.reels().forEach(strip -> out.add(strip.histogram()));
        return out;
    }
}
