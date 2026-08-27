package com.slotengine.engine.config;

import com.slotengine.model.GameDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merges a compact overlay (core knobs, weights, pays, math) onto a compiled game
 * or onto another document. Used for RTP variants and the live math workbench.
 */
public final class GameDocumentOverlay {

    private final JsonGameLoader loader;

    public GameDocumentOverlay(JsonGameLoader loader) {
        this.loader = loader;
    }

    public GameDefinition apply(GameDefinition base, GameDefinitionDocument overlay) {
        GameDefinitionDocument merged = loader.toDocument(base);
        merge(merged, overlay);
        if (overlay.id != null && !overlay.id.isBlank()) {
            merged.id = overlay.id;
        }
        if (overlay.name != null && !overlay.name.isBlank()) {
            merged.name = overlay.name;
        }
        merged.template = null;
        merged.extendsId = null;
        return loader.fromDocument(merged);
    }

    public void merge(GameDefinitionDocument base, GameDefinitionDocument overlay) {
        if (overlay == null) {
            return;
        }
        if (overlay.version != null) {
            base.version = overlay.version;
        }
        if (overlay.theme != null) {
            base.theme = overlay.theme;
        }
        if (overlay.grid != null) {
            base.grid = overlay.grid;
        }
        if (overlay.evaluation != null) {
            base.evaluation = overlay.evaluation;
        }
        if (overlay.paylinesPreset != null) {
            base.paylinesPreset = overlay.paylinesPreset;
        }
        if (overlay.paylines != null) {
            base.paylines = overlay.paylines;
        }
        if (overlay.symbols != null) {
            base.symbols = overlay.symbols;
        }
        if (overlay.paytable != null) {
            base.paytable = mergePaytable(base.paytable, overlay.paytable);
        }
        if (overlay.reels != null) {
            base.reels = mergeReels(base.reels, overlay.reels);
        }
        if (overlay.features != null) {
            base.features = overlay.features;
        }
        if (overlay.bets != null) {
            base.bets = overlay.bets;
        }
        if (overlay.math != null) {
            base.math = mergeMath(base.math, overlay.math);
        }
        if (overlay.core != null) {
            applyCore(base, overlay.core);
        }
        if (overlay.bothWays) {
            base.bothWays = true;
        }
    }

    public static void applyCore(GameDefinitionDocument doc, GameDefinitionDocument.CoreDoc core) {
        if (core == null) {
            return;
        }
        if (core.evaluation != null) {
            doc.evaluation = core.evaluation;
        }
        if (core.bothWays != null) {
            doc.bothWays = core.bothWays;
        }
        if (core.maxWinMultiplier != null) {
            if (doc.math == null) {
                doc.math = new GameDefinitionDocument.MathDoc();
            }
            doc.math.maxWinMultiplier = core.maxWinMultiplier;
        }
        if (doc.features == null) {
            doc.features = new GameDefinitionDocument.FeaturesDoc();
        }
        GameDefinitionDocument.FeaturesDoc features = doc.features;
        if (core.expandingWilds != null) {
            features.expandingWilds = core.expandingWilds;
        }
        if (core.freeSpinsMultiplier != null
                || core.freeSpinsAwards != null
                || core.retrigger != null
                || core.retriggerSpins != null
                || core.stickyWilds != null
                || core.useFreeReels != null) {
            if (features.freeSpins == null) {
                features.freeSpins = new GameDefinitionDocument.FreeSpinsDoc();
                features.freeSpins.triggerSymbol = "SCATTER";
            }
            GameDefinitionDocument.FreeSpinsDoc fs = features.freeSpins;
            if (core.freeSpinsMultiplier != null) {
                fs.multiplier = core.freeSpinsMultiplier;
            }
            if (core.freeSpinsAwards != null) {
                fs.awards = core.freeSpinsAwards;
            }
            if (core.retrigger != null) {
                fs.retrigger = core.retrigger;
            }
            if (core.retriggerSpins != null) {
                fs.retriggerSpins = core.retriggerSpins;
            }
            if (core.stickyWilds != null) {
                fs.stickyWilds = core.stickyWilds;
            }
            if (core.useFreeReels != null) {
                fs.useFreeReels = core.useFreeReels;
            }
        }
        if (core.buyBonusCostMultiplier != null || core.buyBonusScatters != null) {
            if (features.buyBonus == null) {
                features.buyBonus = new GameDefinitionDocument.BuyBonusDoc();
                features.buyBonus.costMultiplier = 100;
                features.buyBonus.guaranteedScatterCount = 3;
            }
            if (core.buyBonusCostMultiplier != null) {
                features.buyBonus.costMultiplier = core.buyBonusCostMultiplier;
            }
            if (core.buyBonusScatters != null) {
                features.buyBonus.guaranteedScatterCount = core.buyBonusScatters;
            }
        }
        if (core.anteCostMultiplier != null) {
            features.ante = new GameDefinitionDocument.AnteDoc();
            features.ante.costMultiplier = core.anteCostMultiplier;
        }
        if (core.cascade != null) {
            if (!core.cascade) {
                features.cascade = new GameDefinitionDocument.CascadeDoc();
                features.cascade.enabled = false;
            } else {
                if (features.cascade == null) {
                    features.cascade = new GameDefinitionDocument.CascadeDoc();
                }
                features.cascade.enabled = true;
                if (core.cascadeStartMultiplier != null) {
                    features.cascade.startMultiplier = core.cascadeStartMultiplier;
                }
                if (core.cascadeIncrement != null) {
                    features.cascade.increment = core.cascadeIncrement;
                }
                if (core.cascadeMaxMultiplier != null) {
                    features.cascade.maxMultiplier = core.cascadeMaxMultiplier;
                }
            }
        }
    }

    private static GameDefinitionDocument.PaytableDoc mergePaytable(
            GameDefinitionDocument.PaytableDoc base,
            GameDefinitionDocument.PaytableDoc overlay
    ) {
        if (base == null) {
            return overlay;
        }
        if (overlay.lineUnit != null) {
            base.lineUnit = overlay.lineUnit;
        }
        if (overlay.scatterUnit != null) {
            base.scatterUnit = overlay.scatterUnit;
        }
        if (overlay.line != null) {
            if (base.line == null) {
                base.line = new LinkedHashMap<>();
            }
            mergePayRows(base.line, overlay.line);
        }
        if (overlay.scatter != null) {
            if (base.scatter == null) {
                base.scatter = new LinkedHashMap<>();
            }
            mergePayRows(base.scatter, overlay.scatter);
        }
        return base;
    }

    private static void mergePayRows(
            Map<String, Map<String, Integer>> target,
            Map<String, Map<String, Integer>> overlay
    ) {
        overlay.forEach((symbol, row) -> {
            Map<String, Integer> existing = target.computeIfAbsent(symbol, k -> new LinkedHashMap<>());
            existing.putAll(row);
        });
    }

    private static GameDefinitionDocument.ReelsDoc mergeReels(
            GameDefinitionDocument.ReelsDoc base,
            GameDefinitionDocument.ReelsDoc overlay
    ) {
        if (base == null) {
            return overlay;
        }
        if (overlay.base != null) {
            base.base = overlay.base;
        }
        if (overlay.free != null) {
            base.free = overlay.free;
        }
        if (overlay.ante != null) {
            base.ante = overlay.ante;
        }
        if (overlay.baseWeights != null) {
            base.baseWeights = overlay.baseWeights;
            base.base = null;
        }
        if (overlay.freeWeights != null) {
            base.freeWeights = overlay.freeWeights;
            base.free = null;
        }
        if (overlay.anteWeights != null) {
            base.anteWeights = overlay.anteWeights;
            base.ante = null;
        }
        return base;
    }

    private static GameDefinitionDocument.MathDoc mergeMath(
            GameDefinitionDocument.MathDoc base,
            GameDefinitionDocument.MathDoc overlay
    ) {
        if (base == null) {
            return overlay;
        }
        if (overlay.targetRtp != 0) {
            base.targetRtp = overlay.targetRtp;
        }
        if (overlay.volatility != null) {
            base.volatility = overlay.volatility;
        }
        if (overlay.maxWinMultiplier != 0) {
            base.maxWinMultiplier = overlay.maxWinMultiplier;
        }
        if (overlay.hitFrequencyPercent != null) {
            base.hitFrequencyPercent = overlay.hitFrequencyPercent;
        }
        if (overlay.notes != null) {
            base.notes = overlay.notes;
        }
        return base;
    }
}
