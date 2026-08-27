package com.slotengine.api.web;

import com.slotengine.api.config.SlotProperties;
import com.slotengine.engine.catalog.GameCatalog;
import com.slotengine.math.BaseGameEnumerator;
import com.slotengine.math.MonteCarloSimulator;
import com.slotengine.math.SimulationConfig;
import com.slotengine.math.SimulationReport;
import com.slotengine.api.dto.ApiDtos;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.PlayMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/math")
@Tag(name = "Math", description = "RTP / volatility workbench")
public class MathController {

    private static final long MAX_SPINS = 5_000_000L;

    private final GameCatalog catalog;
    private final MonteCarloSimulator simulator;
    private final BaseGameEnumerator enumerator;
    private final SlotProperties properties;

    public MathController(
            GameCatalog catalog,
            MonteCarloSimulator simulator,
            BaseGameEnumerator enumerator,
            SlotProperties properties
    ) {
        this.catalog = catalog;
        this.simulator = simulator;
        this.enumerator = enumerator;
        this.properties = properties;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Monte Carlo full-game RTP, hit rate, volatility")
    public Map<String, Object> simulate(@Valid @RequestBody ApiDtos.SimulateRequest request) {
        assertMathEnabled();
        if (request.spins() > MAX_SPINS) {
            throw new IllegalArgumentException("spins cap is " + MAX_SPINS);
        }
        GameDefinition game = catalog.require(request.gameId());
        long bet = request.bet() == null ? defaultBet(game) : request.bet();
        PlayMode mode = request.mode() == null ? PlayMode.NORMAL : request.mode();
        long seed = request.seed() == null ? 1L : request.seed();
        SimulationReport report = simulator.simulate(
                game,
                new SimulationConfig(request.spins(), bet, seed, mode, 0)
        );
        Map<String, Object> body = report.asMap();
        body.put("targetRtp", game.math().targetRtp());
        body.put("targetVolatility", game.math().volatility().name());
        return body;
    }

    @PostMapping("/enumerate")
    @Operation(summary = "Exact base-game cycle (no free-spin play)")
    public Map<String, Object> enumerate(@Valid @RequestBody ApiDtos.EnumerateRequest request) {
        assertMathEnabled();
        GameDefinition game = catalog.require(request.gameId());
        long bet = request.bet() == null ? defaultBet(game) : request.bet();
        return enumerator.enumerate(game, bet).asMap();
    }

    private void assertMathEnabled() {
        if (!properties.mathApiEnabled()) {
            throw ForbiddenInLiveException.math();
        }
    }

    private static long defaultBet(GameDefinition game) {
        if (game.lineCount() > 0) {
            return game.lineCount();
        }
        return 1;
    }
}
