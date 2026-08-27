package com.slotengine.engine.eval;

import com.slotengine.model.GameDefinition;
import com.slotengine.model.Payline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaylineEvaluatorTest {

    private final PaylineEvaluator evaluator = new PaylineEvaluator();

    @Test
    void threeOfAKindOnSingleLine() {
        Window window = window(
                col("K", "A", "K"),
                col("K", "A", "K"),
                col("K", "A", "K"),
                col("A", "K", "A"),
                col("A", "K", "A")
        );
        List<Win> wins = evaluator.evaluate(toy(), window, 10, 1);
        assertThat(wins).hasSize(1);
        assertThat(wins.get(0).symbolId()).isEqualTo("A");
        assertThat(wins.get(0).ofAKind()).isEqualTo(3);
        assertThat(wins.get(0).amount()).isEqualTo(5 * 10L);
    }

    @Test
    void wildSubstitutesFromTheLeft() {
        Window window = window(
                col("K", "W", "K"),
                col("K", "A", "K"),
                col("K", "A", "K"),
                col("A", "K", "A"),
                col("A", "K", "A")
        );
        List<Win> wins = evaluator.evaluate(toy(), window, 10, 1);
        assertThat(wins).hasSize(1);
        assertThat(wins.get(0).symbolId()).isEqualTo("A");
        assertThat(wins.get(0).ofAKind()).isEqualTo(3);
    }

    @Test
    void allWildsUseWildPaytable() {
        Window window = window(
                col("A", "W", "A"),
                col("A", "W", "A"),
                col("A", "W", "A"),
                col("A", "W", "A"),
                col("A", "W", "A")
        );
        List<Win> wins = evaluator.evaluate(toy(), window, 10, 1);
        assertThat(wins).hasSize(1);
        assertThat(wins.get(0).symbolId()).isEqualTo("W");
        assertThat(wins.get(0).ofAKind()).isEqualTo(5);
        assertThat(wins.get(0).amount()).isEqualTo(100 * 10L);
    }

    @Test
    void scatterDoesNotFormALine() {
        Window window = window(
                col("K", "S", "K"),
                col("K", "S", "K"),
                col("K", "S", "K"),
                col("K", "A", "K"),
                col("K", "A", "K")
        );
        assertThat(evaluator.evaluate(toy(), window, 10, 1)).isEmpty();
    }

    @Test
    void extraMultiplierScalesLineWin() {
        Window window = window(
                col("K", "A", "K"),
                col("K", "A", "K"),
                col("K", "A", "K"),
                col("K", "A", "K"),
                col("K", "A", "K")
        );
        List<Win> wins = evaluator.evaluate(toy(), window, 10, 3);
        assertThat(wins.get(0).amount()).isEqualTo(20 * 10L * 3);
        assertThat(wins.get(0).multiplier()).isEqualTo(3);
    }

    static GameDefinition toy() {
        return GameDefinition.builder("toy-lines")
                .grid(5, 3)
                .wild("W")
                .scatter("S")
                .symbol("A")
                .symbol("K")
                .paylines(List.of(Payline.of(0, 1, 1, 1, 1, 1)))
                .linePay("W", 3, 10, 4, 20, 5, 100)
                .linePay("A", 3, 5, 4, 10, 5, 20)
                .scatterPay("S", 3, 2)
                .baseReels(b -> {
                    b.reel("A", "K", "W", "S", "A", "K");
                    b.reel("A", "K", "W", "S", "A", "K");
                    b.reel("A", "K", "W", "S", "A", "K");
                    b.reel("A", "K", "W", "S", "A", "K");
                    b.reel("A", "K", "W", "S", "A", "K");
                })
                .build();
    }

    static Window window(String[]... columns) {
        return new Window(columns, new int[columns.length]);
    }

    static String[] col(String... rows) {
        return rows;
    }
}
