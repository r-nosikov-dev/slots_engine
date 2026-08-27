package com.slotengine.engine.catalog;

import com.slotengine.engine.config.GameDefinitionDocument;
import com.slotengine.engine.config.JsonGameLoader;
import com.slotengine.model.GameDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of compiled games. Starts with the built-in catalog and can ingest
 * extra JSON definitions from a directory — that is how an editor/frontend
 * publishes a new math file without a rebuild.
 *
 * <p>Files with {@code extendsId} are applied as overlays in a second pass, so
 * an RTP variant can be a 15-line JSON file that only changes weights and core knobs.
 */
public final class GameCatalog {

    private final Map<String, GameDefinition> games = new LinkedHashMap<>();
    private final JsonGameLoader loader = new JsonGameLoader();

    public GameCatalog() {
        register(CatalogGames.classicFruits());
        register(CatalogGames.goldenLynx());
        register(CatalogGames.neonWays());
        register(CatalogGames.gemFall());
    }

    public static GameCatalog builtin() {
        return new GameCatalog();
    }

    public synchronized GameCatalog register(GameDefinition game) {
        games.put(game.id(), game);
        return this;
    }

    public synchronized GameCatalog loadJson(Path path) throws IOException {
        registerDocument(loader.readDocument(path));
        return this;
    }

    public synchronized GameCatalog loadJson(InputStream in) throws IOException {
        registerDocument(loader.readDocument(in));
        return this;
    }

    public synchronized GameCatalog loadDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return this;
        }
        List<GameDefinitionDocument> bases = new ArrayList<>();
        List<GameDefinitionDocument> overlays = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            for (Path path : stream.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                GameDefinitionDocument doc = loader.readDocument(path);
                if (doc.extendsId != null && !doc.extendsId.isBlank()) {
                    overlays.add(doc);
                } else {
                    bases.add(doc);
                }
            }
        }
        for (GameDefinitionDocument doc : bases) {
            registerDocument(doc);
        }
        for (GameDefinitionDocument doc : overlays) {
            GameDefinition parent = require(doc.extendsId);
            register(loader.overlay().apply(parent, doc));
        }
        return this;
    }

    private void registerDocument(GameDefinitionDocument doc) {
        if (doc.extendsId != null && !doc.extendsId.isBlank()) {
            register(loader.overlay().apply(require(doc.extendsId), doc));
        } else {
            register(loader.fromDocument(doc));
        }
    }

    public synchronized Optional<GameDefinition> find(String id) {
        return Optional.ofNullable(games.get(id));
    }

    public GameDefinition require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown game: " + id));
    }

    public synchronized Collection<GameDefinition> all() {
        return List.copyOf(games.values());
    }

    public JsonGameLoader loader() {
        return loader;
    }
}
