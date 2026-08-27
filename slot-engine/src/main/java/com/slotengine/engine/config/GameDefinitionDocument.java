package com.slotengine.engine.config;

import java.util.List;
import java.util.Map;

/**
 * Jackson-friendly JSON document for a game definition.
 * This is the interchange format between the engine, math tools and a future visual editor.
 */
public class GameDefinitionDocument {

    public String id;
    public String name;
    public String version;
    public String theme;
    /** Skeleton: classic-10, classic-20, ways-243, cascade-20. */
    public String template;
    /** Clone another catalog game and apply this document as an overlay. */
    public String extendsId;
    public Grid grid;
    public String evaluation;
    public boolean bothWays;
    public List<SymbolDoc> symbols;
    public PaytableDoc paytable;
    public String paylinesPreset;
    public List<int[]> paylines;
    public ReelsDoc reels;
    public FeaturesDoc features;
    /** Compact engine knobs — applied after {@code features}. */
    public CoreDoc core;
    public BetsDoc bets;
    public MathDoc math;

    public static class Grid {
        public int reels;
        public int rows;
    }

    public static class SymbolDoc {
        public String id;
        public String displayName;
        public String kind;
        public int wildMultiplier = 1;
        public int tier = 1;
        public List<String> substitutes;
        public List<String> cannotReplace;
    }

    public static class PaytableDoc {
        public String lineUnit;
        public String scatterUnit;
        public Map<String, Map<String, Integer>> line;
        public Map<String, Map<String, Integer>> scatter;
    }

    public static class ReelsDoc {
        public List<List<String>> base;
        public List<List<String>> free;
        public List<List<String>> ante;
        /** Preferred for math tuning. Wins over explicit strips when both are set. */
        public List<Map<String, Integer>> baseWeights;
        public List<Map<String, Integer>> freeWeights;
        public List<Map<String, Integer>> anteWeights;
    }

    /**
     * Fast per-game engine + math controls. Designers edit these instead of the
     * full feature tree when iterating RTP and volatility.
     */
    public static class CoreDoc {
        public String evaluation;
        public Boolean bothWays;
        public Boolean expandingWilds;
        public Integer maxWinMultiplier;
        public Integer freeSpinsMultiplier;
        public Map<String, Integer> freeSpinsAwards;
        public Boolean retrigger;
        public Integer retriggerSpins;
        public Boolean stickyWilds;
        public Boolean useFreeReels;
        public Long buyBonusCostMultiplier;
        public Integer buyBonusScatters;
        public Double anteCostMultiplier;
        public Boolean cascade;
        public Integer cascadeStartMultiplier;
        public Integer cascadeIncrement;
        public Integer cascadeMaxMultiplier;
    }

    public static class FeaturesDoc {
        public FreeSpinsDoc freeSpins;
        public BuyBonusDoc buyBonus;
        public AnteDoc ante;
        public CascadeDoc cascade;
        public boolean expandingWilds;
        public JackpotDoc jackpot;
    }

    public static class FreeSpinsDoc {
        public String triggerSymbol;
        public int minTriggerCount = 3;
        public Map<String, Integer> awards;
        public boolean retrigger = true;
        public int retriggerSpins;
        public int multiplier = 1;
        public boolean useFreeReels;
        public boolean stickyWilds;
        public int maxAwardedSpins = 300;
    }

    public static class BuyBonusDoc {
        public long costMultiplier;
        public int guaranteedScatterCount;
    }

    public static class AnteDoc {
        public double costMultiplier;
    }

    public static class CascadeDoc {
        public boolean enabled;
        public int startMultiplier = 1;
        public int increment = 1;
        public int maxMultiplier = 8;
    }

    public static class JackpotDoc {
        public boolean enabled;
        public List<JackpotTierDoc> tiers;
    }

    public static class JackpotTierDoc {
        public String id;
        public long seedCredits;
        public long contributionPerMillion;
        public double mysteryChancePerSpin;
    }

    public static class BetsDoc {
        public List<Long> coinValues;
        public long defaultCoinValue;
        public int defaultCoinsPerLine = 1;
        public long minTotalBet;
        public long maxTotalBet;
    }

    public static class MathDoc {
        public double targetRtp;
        public String volatility;
        public int maxWinMultiplier;
        public Integer hitFrequencyPercent;
        public String notes;
    }
}
