package com.slotengine.api.web;

import com.slotengine.SlotsEngine;
import com.slotengine.api.config.SlotProperties;
import com.slotengine.api.dto.ApiDtos;
import com.slotengine.engine.catalog.GameCatalog;
import com.slotengine.engine.config.GameDefinitionDocument;
import com.slotengine.model.GameDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "Games", description = "Catalog and engine interchange")
public class GameController {

    private final GameCatalog catalog;
    private final SlotsEngine slotsEngine;
    private final SlotProperties properties;

    public GameController(GameCatalog catalog, SlotsEngine slotsEngine, SlotProperties properties) {
        this.catalog = catalog;
        this.slotsEngine = slotsEngine;
        this.properties = properties;
    }

    @GetMapping
    @Operation(summary = "List compiled games")
    public List<ApiDtos.GameSummary> list() {
        return catalog.all().stream().map(ApiDtos.GameSummary::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Game summary")
    public ApiDtos.GameSummary get(@PathVariable String id) {
        return ApiDtos.GameSummary.from(catalog.require(id));
    }

    @GetMapping("/{id}/definition")
    @Operation(summary = "Full math definition (JSON engine document)")
    public GameDefinitionDocument definition(@PathVariable String id) {
        return slotsEngine.json().toDocument(catalog.require(id));
    }

    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Compile and register a JSON game definition")
    public ApiDtos.GameSummary importGame(@RequestBody GameDefinitionDocument document) {
        if (!properties.studioApiEnabled()) {
            throw ForbiddenInLiveException.studio();
        }
        GameDefinition game = slotsEngine.json().fromDocument(document);
        catalog.register(game);
        return ApiDtos.GameSummary.from(game);
    }

    @GetMapping("/{id}/symbols")
    public Map<String, Object> symbols(@PathVariable String id) {
        GameDefinition game = catalog.require(id);
        return Map.of(
                "id", game.id(),
                "symbols", game.symbols().values()
        );
    }
}
