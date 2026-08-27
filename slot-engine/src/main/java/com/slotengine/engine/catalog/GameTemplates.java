package com.slotengine.engine.catalog;

import com.slotengine.model.BetConfig;
import com.slotengine.model.GameBuilder;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.PayUnit;
import com.slotengine.model.Paylines;
import com.slotengine.model.VolatilityClass;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Instantiates a playable {@link GameBuilder} from a {@link GameTemplateId}.
 * Tune afterwards with weights, pays and {@code core} knobs — that is the fast path
 * for a new title.
 */
public final class GameTemplates {

    private GameTemplates() {
    }

    public static GameBuilder start(String gameId, GameTemplateId template) {
        return switch (template) {
            case CLASSIC_10 -> classic10(gameId);
            case CLASSIC_20 -> classic20(gameId);
            case WAYS_243 -> ways243(gameId);
            case CASCADE_20 -> cascade20(gameId);
        };
    }

    public static GameDefinition build(String gameId, GameTemplateId template) {
        return start(gameId, template).build();
    }

    public static List<Map<String, String>> describe() {
        return Arrays.stream(GameTemplateId.values())
                .map(t -> Map.of(
                        "id", t.slug(),
                        "name", t.name(),
                        "description", t.description()
                ))
                .toList();
    }

    private static GameBuilder classic10(String id) {
        return GameDefinition.builder(id)
                .named(id)
                .theme("classic-fruits")
                .grid(5, 3)
                .wild("WILD")
                .high("SEVEN")
                .high("BAR")
                .high("BELL")
                .low("PLUM")
                .low("ORANGE")
                .low("LEMON")
                .low("CHERRY")
                .standard10Paylines()
                .linePay("WILD", 3, 50, 4, 150, 5, 500)
                .linePay("SEVEN", 3, 30, 4, 80, 5, 300)
                .linePay("BAR", 3, 15, 4, 40, 5, 150)
                .linePay("BELL", 3, 15, 4, 30, 5, 100)
                .linePay("PLUM", 3, 10, 4, 20, 5, 60)
                .linePay("ORANGE", 3, 8, 4, 15, 5, 40)
                .linePay("LEMON", 3, 5, 4, 10, 5, 25)
                .linePay("CHERRY", 2, 2, 3, 5, 4, 10, 5, 20)
                .baseReels(b -> repeatWeights(b, 5, WeightMap.of(
                        "WILD", 1, "SEVEN", 1, "BAR", 2, "BELL", 2,
                        "PLUM", 3, "ORANGE", 3, "LEMON", 4, "CHERRY", 4
                )))
                .bets(new BetConfig(List.of(1L, 2L, 5L, 10L), 1L, 1, 10, 10_000))
                .targetRtp(0.9600)
                .volatility(VolatilityClass.LOW)
                .maxWin(1000);
    }

    private static GameBuilder classic20(String id) {
        return GameDefinition.builder(id)
                .named(id)
                .theme("video-slot")
                .grid(5, 3)
                .wild("WILD")
                .scatter("SCATTER")
                .high("H1")
                .high("H2")
                .high("H3")
                .low("A")
                .low("K")
                .low("Q")
                .low("J")
                .low("TEN")
                .low("NINE")
                .paylines(Paylines.standard20())
                .linePay("WILD", 3, 50, 4, 200, 5, 1000)
                .linePay("H1", 3, 30, 4, 100, 5, 500)
                .linePay("H2", 3, 20, 4, 50, 5, 200)
                .linePay("H3", 3, 15, 4, 40, 5, 150)
                .linePay("A", 3, 10, 4, 25, 5, 100)
                .linePay("K", 3, 8, 4, 20, 5, 80)
                .linePay("Q", 3, 6, 4, 15, 5, 60)
                .linePay("J", 3, 5, 4, 12, 5, 50)
                .linePay("TEN", 3, 5, 4, 10, 5, 40)
                .linePay("NINE", 3, 4, 4, 8, 5, 30)
                .scatterPay("SCATTER", 3, 2, 4, 10, 5, 50)
                .baseReels(b -> {
                    b.reelWeights(classic20Weights(0, false));
                    b.reelWeights(classic20Weights(1, false));
                    b.reelWeights(classic20Weights(2, false));
                    b.reelWeights(classic20Weights(3, false));
                    b.reelWeights(classic20Weights(4, false));
                })
                .freeReels(b -> {
                    b.reelWeights(classic20Weights(0, true));
                    b.reelWeights(classic20Weights(1, true));
                    b.reelWeights(classic20Weights(2, true));
                    b.reelWeights(classic20Weights(3, true));
                    b.reelWeights(classic20Weights(4, true));
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
                .bets(new BetConfig(List.of(1L, 2L, 5L, 10L, 25L, 50L, 100L), 1L, 1, 20, 100_000))
                .targetRtp(0.9600)
                .volatility(VolatilityClass.MEDIUM)
                .maxWin(5000);
    }

    private static GameBuilder ways243(String id) {
        return GameDefinition.builder(id)
                .named(id)
                .theme("ways")
                .grid(5, 3)
                .ways()
                .linePayUnit(PayUnit.TOTAL_BET)
                .wild("WILD", 2)
                .scatter("SCATTER")
                .high("H1")
                .high("H2")
                .high("H3")
                .low("A")
                .low("K")
                .low("Q")
                .low("J")
                .low("TEN")
                .linePay("WILD", 3, 10, 4, 40, 5, 200)
                .linePay("H1", 3, 5, 4, 20, 5, 80)
                .linePay("H2", 3, 3, 4, 10, 5, 40)
                .linePay("H3", 3, 2, 4, 8, 5, 25)
                .linePay("A", 3, 1, 4, 4, 5, 15)
                .linePay("K", 3, 1, 4, 3, 5, 12)
                .linePay("Q", 3, 1, 4, 2, 5, 10)
                .linePay("J", 3, 1, 4, 2, 5, 8)
                .linePay("TEN", 3, 1, 4, 2, 5, 5)
                .scatterPay("SCATTER", 3, 1, 4, 5, 5, 20)
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
                .targetRtp(0.9600)
                .volatility(VolatilityClass.HIGH)
                .maxWin(5000);
    }

    private static GameBuilder cascade20(String id) {
        return GameDefinition.builder(id)
                .named(id)
                .theme("tumble")
                .grid(5, 3)
                .wild("WILD")
                .scatter("SCATTER")
                .high("H1")
                .high("H2")
                .high("H3")
                .low("L1")
                .low("L2")
                .low("L3")
                .paylines(Paylines.standard20())
                .linePay("WILD", 3, 40, 4, 150, 5, 800)
                .linePay("H1", 3, 25, 4, 80, 5, 400)
                .linePay("H2", 3, 15, 4, 40, 5, 150)
                .linePay("H3", 3, 10, 4, 30, 5, 100)
                .linePay("L1", 3, 8, 4, 20, 5, 60)
                .linePay("L2", 3, 5, 4, 15, 5, 40)
                .linePay("L3", 3, 5, 4, 10, 5, 25)
                .scatterPay("SCATTER", 3, 2, 4, 8, 5, 30)
                .baseReels(b -> repeatWeights(b, 5, WeightMap.of(
                        "WILD", 1, "SCATTER", 1, "H1", 2, "H2", 3, "H3", 3,
                        "L1", 4, "L2", 5, "L3", 6
                )))
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
                .bets(new BetConfig(List.of(1L, 2L, 5L, 10L, 25L), 1L, 1, 20, 50_000))
                .targetRtp(0.9600)
                .volatility(VolatilityClass.MEDIUM)
                .maxWin(3000);
    }

    private static Map<String, Integer> classic20Weights(int reel, boolean free) {
        int wild = free ? (reel == 0 ? 1 : 3) : (reel == 0 ? 0 : 1);
        int scatter = free ? 1 : (reel % 2 == 0 ? 2 : 1);
        return WeightMap.of(
                "WILD", wild,
                "SCATTER", scatter,
                "H1", free ? 3 : 2,
                "H2", 2,
                "H3", 3,
                "A", 4,
                "K", 4,
                "Q", 5,
                "J", 5,
                "TEN", 6,
                "NINE", 6
        );
    }

    private static Map<String, Integer> waysWeights(boolean free, int reel) {
        int wild = free ? (reel == 0 ? 2 : 4) : (reel == 0 ? 0 : 2);
        return WeightMap.of(
                "WILD", wild,
                "SCATTER", free ? 2 : 1,
                "H1", free ? 3 : 2,
                "H2", 3,
                "H3", 3,
                "A", 4,
                "K", 4,
                "Q", 5,
                "J", 5,
                "TEN", 6
        );
    }

    private static void repeatWeights(
            com.slotengine.model.ReelSet.Builder builder,
            int reels,
            Map<String, Integer> weights
    ) {
        for (int i = 0; i < reels; i++) {
            builder.reelWeights(new LinkedHashMap<>(weights));
        }
    }
}
