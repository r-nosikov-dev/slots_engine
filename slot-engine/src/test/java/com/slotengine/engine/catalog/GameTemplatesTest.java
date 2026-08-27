package com.slotengine.engine.catalog;

import com.slotengine.engine.PlayRequest;
import com.slotengine.engine.SlotEngine;
import com.slotengine.engine.config.GameDefinitionDocument;
import com.slotengine.engine.config.JsonGameLoader;
import com.slotengine.model.GameDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameTemplatesTest {

    private final SlotEngine engine = new SlotEngine();

    @Test
    void everyTemplateBuildsAndSpins() {
        for (GameTemplateId template : GameTemplateId.values()) {
            GameDefinition game = GameTemplates.build("t-" + template.slug(), template);
            long bet = game.lineCount() > 0 ? game.lineCount() : 1;
            assertThat(engine.play(game, PlayRequest.spin(bet, 1L)).spins()).isNotEmpty();
        }
    }

    @Test
    void compactTemplateJsonCompiles() throws Exception {
        String json = """
                {
                  "id": "quick-20",
                  "template": "classic-20",
                  "math": { "targetRtp": 0.95, "volatility": "HIGH", "maxWinMultiplier": 2000 },
                  "core": { "freeSpinsMultiplier": 2, "buyBonusCostMultiplier": 80 },
                  "reels": {
                    "baseWeights": [
                      {"WILD": 0, "SCATTER": 1, "H1": 2, "H2": 2, "H3": 3, "A": 4, "K": 4, "Q": 5, "J": 5, "TEN": 6, "NINE": 6},
                      {"WILD": 1, "SCATTER": 1, "H1": 2, "H2": 2, "H3": 3, "A": 4, "K": 4, "Q": 5, "J": 5, "TEN": 6, "NINE": 6},
                      {"WILD": 2, "SCATTER": 1, "H1": 2, "H2": 2, "H3": 3, "A": 4, "K": 4, "Q": 5, "J": 5, "TEN": 6, "NINE": 6},
                      {"WILD": 1, "SCATTER": 1, "H1": 2, "H2": 2, "H3": 3, "A": 4, "K": 4, "Q": 5, "J": 5, "TEN": 6, "NINE": 6},
                      {"WILD": 1, "SCATTER": 1, "H1": 2, "H2": 2, "H3": 3, "A": 4, "K": 4, "Q": 5, "J": 5, "TEN": 6, "NINE": 6}
                    ]
                  }
                }
                """;
        GameDefinition game = new JsonGameLoader().fromJson(json);
        assertThat(game.id()).isEqualTo("quick-20");
        assertThat(game.math().maxWinMultiplier()).isEqualTo(2000);
        assertThat(game.features().freeSpins().orElseThrow().multiplier()).isEqualTo(2);
        assertThat(game.features().buyBonus().orElseThrow().costMultiplier()).isEqualTo(80);
        assertThat(game.baseReels().reel(0).histogram()).doesNotContainKey("WILD");
    }

    @Test
    void overlayChangesCoreWithoutRewritingStrips() {
        GameDefinition base = CatalogGames.goldenLynx();
        int originalWilds = base.baseReels().reel(1).histogram().getOrDefault("WILD", 0);
        GameDefinitionDocument overlay = new GameDefinitionDocument();
        overlay.id = "golden-lynx-94";
        overlay.core = new GameDefinitionDocument.CoreDoc();
        overlay.core.freeSpinsMultiplier = 2;
        overlay.core.buyBonusCostMultiplier = 80L;
        overlay.math = new GameDefinitionDocument.MathDoc();
        overlay.math.targetRtp = 0.94;
        overlay.math.volatility = "HIGH";
        overlay.math.maxWinMultiplier = 4000;
        GameDefinition variant = new JsonGameLoader().overlay().apply(base, overlay);
        assertThat(variant.id()).isEqualTo("golden-lynx-94");
        assertThat(variant.features().freeSpins().orElseThrow().multiplier()).isEqualTo(2);
        assertThat(variant.features().buyBonus().orElseThrow().costMultiplier()).isEqualTo(80);
        assertThat(variant.math().maxWinMultiplier()).isEqualTo(4000);
        assertThat(variant.baseReels().reel(1).histogram().getOrDefault("WILD", 0)).isEqualTo(originalWilds);
    }

    @Test
    void catalogAppliesExtendsFiles() throws Exception {
        GameCatalog catalog = GameCatalog.builtin();
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("slot-games");
        java.nio.file.Files.writeString(dir.resolve("child.json"), """
                { "id": "lynx-lite", "extendsId": "golden-lynx",
                  "core": { "freeSpinsMultiplier": 1 },
                  "math": { "targetRtp": 0.92, "volatility": "LOW", "maxWinMultiplier": 1000 } }
                """);
        catalog.loadDirectory(dir);
        GameDefinition child = catalog.require("lynx-lite");
        assertThat(child.features().freeSpins().orElseThrow().multiplier()).isEqualTo(1);
        assertThat(child.math().maxWinMultiplier()).isEqualTo(1000);
        assertThat(catalog.require("golden-lynx").features().freeSpins().orElseThrow().multiplier()).isEqualTo(3);
    }
}
