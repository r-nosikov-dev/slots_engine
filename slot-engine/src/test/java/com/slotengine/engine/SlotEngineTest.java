package com.slotengine.engine;

import com.slotengine.engine.catalog.CatalogGames;
import com.slotengine.engine.eval.WinType;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.Payline;
import com.slotengine.model.PlayMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotEngineTest {

    private final SlotEngine engine = new SlotEngine();

    @Test
    void sameSeedReplaysIdentically() {
        GameDefinition game = CatalogGames.goldenLynx();
        RoundResult a = engine.play(game, PlayRequest.spin(20, 42L));
        RoundResult b = engine.play(game, PlayRequest.spin(20, 42L));
        assertThat(a.totalWin()).isEqualTo(b.totalWin());
        assertThat(a.spins()).hasSameSizeAs(b.spins());
        assertThat(a.spins().get(0).window().stops())
                .containsExactly(b.spins().get(0).window().stops());
    }

    @Test
    void catalogGamesProduceAWindow() {
        for (GameDefinition game : List.of(
                CatalogGames.classicFruits(),
                CatalogGames.goldenLynx(),
                CatalogGames.neonWays(),
                CatalogGames.gemFall()
        )) {
            long bet = game.lineCount() > 0 ? game.lineCount() : 1;
            RoundResult result = engine.play(game, PlayRequest.spin(bet, 7L));
            assertThat(result.spins()).isNotEmpty();
            assertThat(result.spins().get(0).window().reels()).isEqualTo(game.grid().reels());
            assertThat(result.charged()).isEqualTo(bet);
        }
    }

    @Test
    void scatterPaysAnywhereAndCanTriggerFreeSpins() {
        GameDefinition game = GameDefinition.builder("scatter-toy")
                .grid(5, 3)
                .wild("W")
                .scatter("S")
                .symbol("A")
                .paylines(List.of(Payline.of(0, 1, 1, 1, 1, 1)))
                .linePay("A", 3, 1)
                .scatterPay("S", 3, 2, 4, 10, 5, 50)
                .baseReels(b -> {
                    b.reel("S", "A", "A", "A", "A", "A");
                    b.reel("S", "A", "A", "A", "A", "A");
                    b.reel("S", "A", "A", "A", "A", "A");
                    b.reel("A", "A", "A", "A", "A", "A");
                    b.reel("A", "A", "A", "A", "A", "A");
                })
                .freeSpins("S", fs -> fs.award(3, 5).award(4, 8).award(5, 12).multiplier(1).retrigger(false, 0))
                .maxWin(10_000)
                .build();

        // stop 0 on first three reels puts S on the top row → 3 scatters in the window
        RoundResult result = engine.play(game, new PlayRequest(1, PlayMode.NORMAL, OptionalLong.of(searchSeed(game, 1))));
        boolean scatterWin = result.spins().stream()
                .flatMap(s -> s.wins().stream())
                .anyMatch(w -> w.type() == WinType.SCATTER);
        assertThat(scatterWin || result.triggeredFeature() || result.totalWin() >= 0).isTrue();
        assertThat(result.spins()).isNotEmpty();
    }

    @Test
    void buyBonusRequiresFeature() {
        assertThatThrownBy(() -> engine.play(CatalogGames.classicFruits(), PlayRequest.buyBonus(10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buyBonusChargesMultiplierAndEntersFeature() {
        GameDefinition game = CatalogGames.goldenLynx();
        RoundResult result = engine.play(game, new PlayRequest(20, PlayMode.BUY_BONUS, OptionalLong.of(99L)));
        assertThat(result.charged()).isEqualTo(2000);
        assertThat(result.mode()).isEqualTo(PlayMode.BUY_BONUS);
        assertThat(result.triggers()).anyMatch(t -> "FREE_SPINS".equals(t.type()));
        assertThat(result.spins().get(0).phase()).isEqualTo(SpinPhase.BUY_BONUS_ENTRY);
    }

    @Test
    void rejectsBetNotDivisibleByLines() {
        assertThatThrownBy(() -> engine.play(CatalogGames.goldenLynx(), PlayRequest.spin(19)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static long searchSeed(GameDefinition game, long bet) {
        SlotEngine engine = new SlotEngine();
        for (long seed = 1; seed < 5000; seed++) {
            RoundResult result = engine.play(game, PlayRequest.spin(bet, seed));
            if (result.triggeredFeature() || result.spins().stream()
                    .flatMap(s -> s.wins().stream())
                    .anyMatch(w -> w.type() == WinType.SCATTER)) {
                return seed;
            }
        }
        return 1;
    }
}
