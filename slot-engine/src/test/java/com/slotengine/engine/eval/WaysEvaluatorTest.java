package com.slotengine.engine.eval;

import com.slotengine.model.EvaluationMode;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.PayUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WaysEvaluatorTest {

    private final WaysEvaluator evaluator = new WaysEvaluator();

    @Test
    void waysAreProductOfPerReelCounts() {
        // reel0: 2×A, reel1: 2×A, reel2: 1×A → 4 ways of 3
        Window window = new Window(new String[][]{
                {"A", "A", "K"},
                {"A", "A", "K"},
                {"A", "K", "K"},
                {"K", "K", "K"},
                {"K", "K", "K"}
        }, new int[5]);
        List<Win> wins = evaluator.evaluate(toy(), window, 1, 1);
        Win a = wins.stream().filter(w -> w.symbolId().equals("A")).findFirst().orElseThrow();
        assertThat(a.ofAKind()).isEqualTo(3);
        assertThat(a.waysOrLine()).isEqualTo(4);
        assertThat(a.amount()).isEqualTo(5L * 1 * 4);
    }

    @Test
    void wildsCountTowardWays() {
        Window window = new Window(new String[][]{
                {"A", "K", "K"},
                {"W", "K", "K"},
                {"A", "K", "K"},
                {"K", "K", "K"},
                {"K", "K", "K"}
        }, new int[5]);
        Win a = evaluator.evaluate(toy(), window, 1, 1).stream()
                .filter(w -> w.symbolId().equals("A"))
                .findFirst()
                .orElseThrow();
        assertThat(a.ofAKind()).isEqualTo(3);
        assertThat(a.waysOrLine()).isEqualTo(1);
    }

    static GameDefinition toy() {
        return GameDefinition.builder("toy-ways")
                .grid(5, 3)
                .evaluation(EvaluationMode.WAYS)
                .linePayUnit(PayUnit.TOTAL_BET)
                .wild("W")
                .symbol("A")
                .symbol("K")
                .linePay("A", 3, 5, 4, 10, 5, 20)
                .baseReels(b -> {
                    b.reel("A", "K", "W", "A", "K", "A");
                    b.reel("A", "K", "W", "A", "K", "A");
                    b.reel("A", "K", "W", "A", "K", "A");
                    b.reel("A", "K", "W", "A", "K", "A");
                    b.reel("A", "K", "W", "A", "K", "A");
                })
                .build();
    }
}
