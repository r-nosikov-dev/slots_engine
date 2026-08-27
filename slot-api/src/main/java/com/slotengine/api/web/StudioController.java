package com.slotengine.api.web;

import com.slotengine.SlotsEngine;
import com.slotengine.api.config.ApiConfiguration;
import com.slotengine.api.config.SlotProperties;
import com.slotengine.api.dto.ApiDtos;
import com.slotengine.engine.catalog.GameCatalog;
import com.slotengine.engine.catalog.GameTemplateId;
import com.slotengine.engine.catalog.GameTemplates;
import com.slotengine.engine.config.GameDefinitionDocument;
import com.slotengine.engine.config.MathSnapshot;
import com.slotengine.math.MonteCarloSimulator;
import com.slotengine.math.SimulationConfig;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.PlayMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fast math / core workbench: templates, overlays, preview simulation.
 * Disabled in LIVE together with the rest of the engine.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Studio", description = "Templates and per-game math tuning")
public class StudioController {

    private static final long PREVIEW_SPIN_CAP = 50_000L;

    private final GameCatalog catalog;
    private final SlotsEngine slotsEngine;
    private final SlotProperties properties;
    private final MonteCarloSimulator simulator;

    public StudioController(
            GameCatalog catalog,
            SlotsEngine slotsEngine,
            SlotProperties properties,
            MonteCarloSimulator simulator
    ) {
        this.catalog = catalog;
        this.slotsEngine = slotsEngine;
        this.properties = properties;
        this.simulator = simulator;
    }

    @GetMapping("/templates")
    @Operation(summary = "Slot skeletons: classic-10, classic-20, ways-243, cascade-20")
    public List<Map<String, String>> templates() {
        return GameTemplates.describe();
    }

    @PostMapping("/games/from-template")
    @Operation(summary = "Create a playable game from a template, then tune via PUT /games/{id}/math")
    public ApiDtos.GameSummary fromTemplate(@Valid @RequestBody ApiDtos.FromTemplateRequest request) {
        assertStudio();
        GameDefinition game = GameTemplates.start(request.id(), GameTemplateId.parse(request.template()))
                .named(request.name() == null ? request.id() : request.name())
                .theme(request.theme() == null ? "" : request.theme())
                .build();
        catalog.register(game);
        return ApiDtos.GameSummary.from(game);
    }

    @GetMapping("/games/{id}/math")
    @Operation(summary = "Compact math + core knobs (weights, pays, FS, max win)")
    public MathSnapshot math(@PathVariable String id) {
        return MathSnapshot.of(catalog.require(id));
    }

    @PutMapping(value = "/games/{id}/math", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Apply a math/core overlay and replace the compiled game")
    public MathSnapshot applyMath(@PathVariable String id, @RequestBody GameDefinitionDocument overlay) {
        assertStudio();
        GameDefinition updated = slotsEngine.json().overlay().apply(catalog.require(id), overlay);
        catalog.register(updated);
        return MathSnapshot.of(updated);
    }

    @PostMapping(value = "/games/{id}/math/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Compile overlay without saving; pass spins as a query param to run Monte Carlo")
    public Map<String, Object> preview(
            @PathVariable String id,
            @RequestBody(required = false) GameDefinitionDocument overlay,
            @RequestParam(name = "spins", defaultValue = "0") long spins,
            @RequestParam(name = "bet", required = false) Long bet,
            @RequestParam(name = "mode", required = false) PlayMode mode,
            @RequestParam(name = "seed", required = false) Long seed
    ) {
        assertStudio();
        GameDefinition compiled = overlay == null || isEmpty(overlay)
                ? catalog.require(id)
                : slotsEngine.json().overlay().apply(catalog.require(id), overlay);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("preview", true);
        body.put("registered", false);
        body.put("math", MathSnapshot.of(compiled));
        if (spins > 0) {
            if (!properties.mathApiEnabled()) {
                throw ForbiddenInLiveException.math();
            }
            long n = Math.min(spins, PREVIEW_SPIN_CAP);
            long stake = bet == null ? defaultBet(compiled) : bet;
            PlayMode playMode = mode == null ? PlayMode.NORMAL : mode;
            long rng = seed == null ? 1L : seed;
            body.put("simulation", simulator.simulate(
                    compiled, new SimulationConfig(n, stake, rng, playMode, 0)
            ).asMap());
        }
        return body;
    }

    @PostMapping("/games/reload")
    @Operation(summary = "Reload JSON files from the games directory (overlays included)")
    public List<ApiDtos.GameSummary> reload() throws IOException {
        assertStudio();
        Path dir = ApiConfiguration.resolveGamesDir(properties.getGamesDir());
        if (dir != null) {
            catalog.loadDirectory(dir);
        }
        return catalog.all().stream().map(ApiDtos.GameSummary::from).toList();
    }

    private void assertStudio() {
        if (!properties.studioApiEnabled()) {
            throw ForbiddenInLiveException.studio();
        }
    }

    private static boolean isEmpty(GameDefinitionDocument overlay) {
        return overlay.id == null && overlay.core == null && overlay.math == null
                && overlay.reels == null && overlay.paytable == null && overlay.features == null;
    }

    private static long defaultBet(GameDefinition game) {
        return game.lineCount() > 0 ? game.lineCount() : 1;
    }
}
