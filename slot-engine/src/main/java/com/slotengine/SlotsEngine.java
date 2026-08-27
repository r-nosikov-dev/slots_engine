package com.slotengine;

import com.slotengine.engine.PlayRequest;
import com.slotengine.engine.RoundResult;
import com.slotengine.engine.SlotEngine;
import com.slotengine.engine.catalog.GameCatalog;
import com.slotengine.engine.catalog.GameTemplateId;
import com.slotengine.engine.catalog.GameTemplates;
import com.slotengine.engine.config.JsonGameLoader;
import com.slotengine.model.GameBuilder;
import com.slotengine.model.GameDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Public facade of the engine: author a game, load a JSON math file, spin it.
 * Math simulation lives in {@code slot-math}; the HTTP surface lives in {@code slot-api}.
 */
public final class SlotsEngine {

    private final GameCatalog catalog;
    private final SlotEngine engine;
    private final JsonGameLoader loader;

    public SlotsEngine() {
        this(GameCatalog.builtin());
    }

    public SlotsEngine(GameCatalog catalog) {
        this.catalog = catalog;
        this.engine = new SlotEngine();
        this.loader = catalog.loader();
    }

    public static GameBuilder game(String id) {
        return GameDefinition.builder(id);
    }

    public static GameBuilder fromTemplate(String id, String template) {
        return GameTemplates.start(id, GameTemplateId.parse(template));
    }

    public GameCatalog catalog() {
        return catalog;
    }

    public SlotEngine engine() {
        return engine;
    }

    public JsonGameLoader json() {
        return loader;
    }

    public GameDefinition require(String gameId) {
        return catalog.require(gameId);
    }

    public GameDefinition loadJson(Path path) throws IOException {
        GameDefinition game = loader.load(path);
        catalog.register(game);
        return game;
    }

    public GameDefinition loadJson(InputStream in) throws IOException {
        GameDefinition game = loader.load(in);
        catalog.register(game);
        return game;
    }

    public RoundResult spin(String gameId, long totalBet) {
        return engine.play(catalog.require(gameId), PlayRequest.spin(totalBet));
    }

    public RoundResult spin(String gameId, long totalBet, long seed) {
        return engine.play(catalog.require(gameId), PlayRequest.spin(totalBet, seed));
    }

    public RoundResult play(GameDefinition game, PlayRequest request) {
        return engine.play(game, request);
    }
}
