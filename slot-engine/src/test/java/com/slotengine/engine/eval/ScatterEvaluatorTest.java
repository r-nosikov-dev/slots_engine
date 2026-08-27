package com.slotengine.engine.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.slotengine.engine.eval.PaylineEvaluatorTest.col;
import static com.slotengine.engine.eval.PaylineEvaluatorTest.toy;
import static com.slotengine.engine.eval.PaylineEvaluatorTest.window;
import static org.assertj.core.api.Assertions.assertThat;

class ScatterEvaluatorTest {

    @Test
    void scatterPaysTimesTotalBetRegardlessOfLine() {
        Window w = window(
                col("S", "A", "K"),
                col("K", "S", "A"),
                col("A", "K", "S"),
                col("A", "K", "A"),
                col("A", "K", "A")
        );
        List<Win> wins = new ScatterEvaluator().evaluate(toy(), w, 10);
        assertThat(wins).hasSize(1);
        assertThat(wins.get(0).ofAKind()).isEqualTo(3);
        assertThat(wins.get(0).amount()).isEqualTo(2 * 10L);
    }
}
