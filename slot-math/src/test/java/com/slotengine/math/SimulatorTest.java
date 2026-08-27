package com.slotengine.math;

import com.slotengine.engine.catalog.CatalogGames;
import com.slotengine.model.GameDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatorTest {

    @Test
    void monteCarloIsDeterministicAndReportsRtp() {
        GameDefinition game = CatalogGames.classicFruits();
        MonteCarloSimulator simulator = new MonteCarloSimulator();
        SimulationConfig config = SimulationConfig.of(5_000, 10);
        SimulationReport a = simulator.simulate(game, config);
        SimulationReport b = simulator.simulate(game, config);
        assertThat(a.rtp()).isEqualByComparingTo(b.rtp());
        assertThat(a.totalWagered()).isEqualTo(5_000 * 10);
        assertThat(a.rtp().doubleValue()).isBetween(0.50, 1.40);
        assertThat(a.hitFrequency().doubleValue()).isBetween(0.05, 0.90);
    }

    @Test
    void exactEnumerationMatchesSelf() {
        GameDefinition game = CatalogGames.classicFruits();
        BaseGameEnumerator.EnumerationReport report = new BaseGameEnumerator().enumerate(game, 10);
        assertThat(report.cycle()).isGreaterThan(1_000);
        assertThat(report.baseRtp().doubleValue()).isBetween(0.50, 1.40);
        assertThat(report.lineRtp().doubleValue()).isPositive();
    }
}
