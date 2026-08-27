package com.slotengine.engine.config;

import com.slotengine.engine.catalog.CatalogGames;
import com.slotengine.model.GameDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonGameLoaderTest {

    private final JsonGameLoader loader = new JsonGameLoader();

    @Test
    void roundTripPreservesIdentityAndStrips() throws Exception {
        GameDefinition original = CatalogGames.goldenLynx();
        String json = loader.toJson(original);
        GameDefinition restored = loader.fromJson(json);
        assertThat(restored.id()).isEqualTo(original.id());
        assertThat(restored.grid()).isEqualTo(original.grid());
        assertThat(restored.evaluation()).isEqualTo(original.evaluation());
        assertThat(restored.baseReels().reelCount()).isEqualTo(original.baseReels().reelCount());
        assertThat(restored.baseReels().reel(0).length()).isEqualTo(original.baseReels().reel(0).length());
        assertThat(restored.features().freeSpins()).isPresent();
        assertThat(restored.features().buyBonus()).isPresent();
        assertThat(json).contains("\"id\" : \"golden-lynx\"");
    }

    @Test
    void allCatalogGamesSerialize() throws Exception {
        for (GameDefinition game : java.util.List.of(
                CatalogGames.classicFruits(),
                CatalogGames.goldenLynx(),
                CatalogGames.neonWays(),
                CatalogGames.gemFall()
        )) {
            GameDefinition restored = loader.fromJson(loader.toJson(game));
            assertThat(restored.symbols().keySet()).isEqualTo(game.symbols().keySet());
        }
    }
}
