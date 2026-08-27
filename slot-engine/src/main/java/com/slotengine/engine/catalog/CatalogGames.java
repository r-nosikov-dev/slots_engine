package com.slotengine.engine.catalog;

import com.slotengine.model.BetConfig;
import com.slotengine.model.EvaluationMode;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.PayUnit;
import com.slotengine.model.Paylines;
import com.slotengine.model.VolatilityClass;
import com.slotengine.model.feature.FeatureSet;

import java.util.List;

/**
 * Built-in reference games that exercise the engine: classic lines, video slot
 * with free spins, 243-ways, and cascading tumble.
 */
public final class CatalogGames {

    private CatalogGames() {
    }

    public static GameDefinition classicFruits() {
        return GameDefinition.builder("classic-fruits")
                .named("Classic Fruits")
                .theme("retro-fruits")
                .grid(5, 3)
                .evaluation(EvaluationMode.PAYLINES)
                .wild("WILD")
                .high("SEVEN")
                .high("BAR")
                .high("BELL")
                .low("PLUM")
                .low("ORANGE")
                .low("LEMON")
                .low("CHERRY")
                .standard10Paylines()
                .linePay("WILD", 3, 65, 4, 165, 5, 660)
                .linePay("SEVEN", 3, 35, 4, 80, 5, 330)
                .linePay("BAR", 3, 15, 4, 50, 5, 165)
                .linePay("BELL", 3, 15, 4, 35, 5, 130)
                .linePay("PLUM", 3, 15, 4, 25, 5, 65)
                .linePay("ORANGE", 3, 10, 4, 15, 5, 50)
                .linePay("LEMON", 3, 5, 4, 15, 5, 35)
                .linePay("CHERRY", 2, 3, 3, 7, 4, 10, 5, 25)
                .baseReels(b -> {
                    b.reelWeights(WeightMap.of(
                            "WILD", 1, "SEVEN", 1, "BAR", 2, "BELL", 2,
                            "PLUM", 3, "ORANGE", 3, "LEMON", 4, "CHERRY", 4
                    ));
                    b.reelWeights(WeightMap.of(
                            "WILD", 1, "SEVEN", 1, "BAR", 2, "BELL", 2,
                            "PLUM", 3, "ORANGE", 3, "LEMON", 4, "CHERRY", 4
                    ));
                    b.reelWeights(WeightMap.of(
                            "WILD", 2, "SEVEN", 1, "BAR", 2, "BELL", 2,
                            "PLUM", 3, "ORANGE", 3, "LEMON", 3, "CHERRY", 4
                    ));
                    b.reelWeights(WeightMap.of(
                            "WILD", 1, "SEVEN", 1, "BAR", 2, "BELL", 2,
                            "PLUM", 3, "ORANGE", 3, "LEMON", 4, "CHERRY", 4
                    ));
                    b.reelWeights(WeightMap.of(
                            "SEVEN", 1, "BAR", 2, "BELL", 2,
                            "PLUM", 3, "ORANGE", 4, "LEMON", 4, "CHERRY", 4
                    ));
                })
                .bets(new BetConfig(List.of(1L, 2L, 5L, 10L), 1L, 1, 10, 10_000))
                .targetRtp(0.9475)
                .volatility(VolatilityClass.LOW)
                .maxWin(1000)
                .build();
    }

    public static GameDefinition goldenLynx() {
        return GameDefinition.builder("golden-lynx")
                .named("Golden Lynx")
                .theme("safari-gold")
                .grid(5, 3)
                .wild("WILD")
                .scatter("SCATTER")
                .high("LYNX")
                .high("GOLD")
                .high("MASK")
                .low("A")
                .low("K")
                .low("Q")
                .low("J")
                .low("TEN")
                .low("NINE")
                .paylines(Paylines.standard20())
                .linePay("WILD", 3, 50, 4, 200, 5, 1000)
                .linePay("LYNX", 3, 30, 4, 100, 5, 500)
                .linePay("GOLD", 3, 20, 4, 50, 5, 200)
                .linePay("MASK", 3, 15, 4, 40, 5, 150)
                .linePay("A", 3, 10, 4, 25, 5, 100)
                .linePay("K", 3, 8, 4, 20, 5, 80)
                .linePay("Q", 3, 6, 4, 15, 5, 60)
                .linePay("J", 3, 5, 4, 12, 5, 50)
                .linePay("TEN", 3, 5, 4, 10, 5, 40)
                .linePay("NINE", 3, 4, 4, 8, 5, 30)
                .scatterPay("SCATTER", 3, 2, 4, 10, 5, 50)
                .baseReels(b -> {
                    b.reelWeights(lynxBase(0));
                    b.reelWeights(lynxBase(1));
                    b.reelWeights(lynxBase(2));
                    b.reelWeights(lynxBase(3));
                    b.reelWeights(lynxBase(4));
                })
                .freeReels(b -> {
                    b.reelWeights(lynxFree(0));
                    b.reelWeights(lynxFree(1));
                    b.reelWeights(lynxFree(2));
                    b.reelWeights(lynxFree(3));
                    b.reelWeights(lynxFree(4));
                })
                .freeSpins("SCATTER", fs -> fs
                        .award(3, 10)
                        .award(4, 15)
                        .award(5, 20)
                        .multiplier(3)
                        .retrigger(true, 10)
                        .useFreeReels(true)
                )
                .buyBonus(100, 3)
                .ante(1.25)
                .anteReels(b -> {
                    b.reelWeights(lynxAnte(0));
                    b.reelWeights(lynxAnte(1));
                    b.reelWeights(lynxAnte(2));
                    b.reelWeights(lynxAnte(3));
                    b.reelWeights(lynxAnte(4));
                })
                .bets(new BetConfig(List.of(1L, 2L, 5L, 10L, 25L, 50L, 100L), 1L, 1, 20, 100_000))
                .targetRtp(0.9600)
                .volatility(VolatilityClass.MEDIUM_HIGH)
                .maxWin(5000)
                .build();
    }

    public static GameDefinition neonWays() {
        return GameDefinition.builder("neon-ways")
                .named("Neon Ways")
                .theme("cyber-neon")
                .grid(5, 3)
                .ways()
                .linePayUnit(PayUnit.TOTAL_BET)
                .wild("WILD", 2)
                .scatter("SCATTER")
                .high("NEON")
                .high("CHIP")
                .high("CORE")
                .low("A")
                .low("K")
                .low("Q")
                .low("J")
                .low("TEN")
                .linePay("WILD", 3, 20, 4, 80, 5, 400)
                .linePay("NEON", 3, 10, 4, 40, 5, 200)
                .linePay("CHIP", 3, 5, 4, 20, 5, 80)
                .linePay("CORE", 3, 4, 4, 15, 5, 50)
                .linePay("A", 3, 2, 4, 8, 5, 25)
                .linePay("K", 3, 2, 4, 6, 5, 20)
                .linePay("Q", 3, 1, 4, 5, 5, 15)
                .linePay("J", 3, 1, 4, 4, 5, 12)
                .linePay("TEN", 3, 1, 4, 3, 5, 10)
                .scatterPay("SCATTER", 3, 1, 4, 5, 5, 25)
                .baseReels(b -> {
                    b.reelWeights(waysWeights(false, 0));
                    b.reelWeights(waysWeights(false, 1));
                    b.reelWeights(waysWeights(false, 2));
                    b.reelWeights(waysWeights(false, 3));
                    b.reelWeights(waysWeights(false, 4));
                })
                .freeReels(b -> {
                    b.reelWeights(waysWeights(true, 0));
                    b.reelWeights(waysWeights(true, 1));
                    b.reelWeights(waysWeights(true, 2));
                    b.reelWeights(waysWeights(true, 3));
                    b.reelWeights(waysWeights(true, 4));
                })
                .freeSpins("SCATTER", fs -> fs
                        .award(3, 8)
                        .award(4, 12)
                        .award(5, 16)
                        .multiplier(1)
                        .retrigger(true, 8)
                        .useFreeReels(true)
                        .stickyWilds(true)
                )
                .buyBonus(80, 3)
                .bets(new BetConfig(List.of(1L, 2L, 5L, 10L, 20L, 50L, 100L), 1L, 1, 1, 100_000))
                .targetRtp(0.9610)
                .volatility(VolatilityClass.HIGH)
                .maxWin(5000)
                .build();
    }

    public static GameDefinition gemFall() {
        return GameDefinition.builder("gem-fall")
                .named("Gem Fall")
                .theme("crystal-tumble")
                .grid(5, 3)
                .wild("WILD")
                .scatter("SCATTER")
                .high("DIAMOND")
                .high("RUBY")
                .high("EMERALD")
                .low("SAPPHIRE")
                .low("AMETHYST")
                .low("TOPAZ")
                .paylines(Paylines.standard20())
                .linePay("WILD", 3, 40, 4, 150, 5, 800)
                .linePay("DIAMOND", 3, 25, 4, 80, 5, 400)
                .linePay("RUBY", 3, 15, 4, 40, 5, 150)
                .linePay("EMERALD", 3, 10, 4, 30, 5, 100)
                .linePay("SAPPHIRE", 3, 8, 4, 20, 5, 60)
                .linePay("AMETHYST", 3, 5, 4, 15, 5, 40)
                .linePay("TOPAZ", 3, 5, 4, 10, 5, 25)
                .scatterPay("SCATTER", 3, 2, 4, 8, 5, 30)
                .baseReels(b -> {
                    b.reelWeights(gemWeights(false));
                    b.reelWeights(gemWeights(false));
                    b.reelWeights(gemWeights(true));
                    b.reelWeights(gemWeights(false));
                    b.reelWeights(gemWeights(false));
                })
                .cascade()
                .expandingWilds()
                .freeSpins("SCATTER", fs -> fs
                        .award(3, 8)
                        .award(4, 12)
                        .award(5, 16)
                        .multiplier(1)
                        .retrigger(true, 8)
                )
                .buyBonus(75, 3)
                .jackpot(new FeatureSet.JackpotFeature(true, List.of(
                        new FeatureSet.JackpotTier("MINI", 50, 1_000, 0.001),
                        new FeatureSet.JackpotTier("MINOR", 200, 500, 0.0003),
                        new FeatureSet.JackpotTier("MAJOR", 1_000, 100, 0.00005),
                        new FeatureSet.JackpotTier("GRAND", 10_000, 20, 0.000005)
                )))
                .bets(new BetConfig(List.of(1L, 2L, 5L, 10L, 25L), 1L, 1, 20, 50_000))
                .targetRtp(0.9580)
                .volatility(VolatilityClass.MEDIUM)
                .maxWin(3000)
                .build();
    }

    private static java.util.Map<String, Integer> lynxBase(int reel) {
        int wild = reel == 0 ? 0 : (reel == 2 ? 2 : 1);
        int scatter = reel % 2 == 0 ? 2 : 1;
        return WeightMap.of(
                "WILD", wild,
                "SCATTER", scatter,
                "LYNX", 2,
                "GOLD", 2,
                "MASK", 3,
                "A", 4,
                "K", 4,
                "Q", 5,
                "J", 5,
                "TEN", 6,
                "NINE", 6
        );
    }

    private static java.util.Map<String, Integer> lynxFree(int reel) {
        int wild = reel == 0 ? 1 : 3;
        return WeightMap.of(
                "WILD", wild,
                "SCATTER", 1,
                "LYNX", 3,
                "GOLD", 3,
                "MASK", 3,
                "A", 3,
                "K", 3,
                "Q", 4,
                "J", 4,
                "TEN", 4,
                "NINE", 4
        );
    }

    private static java.util.Map<String, Integer> lynxAnte(int reel) {
        var base = lynxBase(reel);
        base.put("SCATTER", base.getOrDefault("SCATTER", 1) + 1);
        return base;
    }

    private static java.util.Map<String, Integer> waysWeights(boolean free, int reel) {
        int wild = free ? (reel == 0 ? 2 : 4) : (reel == 0 ? 0 : 2);
        return WeightMap.of(
                "WILD", wild,
                "SCATTER", free ? 2 : 1,
                "NEON", free ? 3 : 2,
                "CHIP", 3,
                "CORE", 3,
                "A", 4,
                "K", 4,
                "Q", 5,
                "J", 5,
                "TEN", 6
        );
    }

    private static java.util.Map<String, Integer> gemWeights(boolean extraWild) {
        return WeightMap.of(
                "WILD", extraWild ? 3 : 1,
                "SCATTER", 1,
                "DIAMOND", 2,
                "RUBY", 3,
                "EMERALD", 3,
                "SAPPHIRE", 4,
                "AMETHYST", 5,
                "TOPAZ", 6
        );
    }
}
