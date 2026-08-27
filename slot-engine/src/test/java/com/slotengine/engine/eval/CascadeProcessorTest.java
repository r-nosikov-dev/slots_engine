package com.slotengine.engine.eval;

import com.slotengine.model.GameDefinition;
import com.slotengine.model.Payline;
import com.slotengine.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CascadeProcessorTest {

    @Test
    void winningSymbolsDropAndFillFromAbove() {
        GameDefinition game = GameDefinition.builder("cascade-toy")
                .grid(3, 3)
                .ways()
                .symbol("A")
                .symbol("K")
                .linePay("A", 3, 1)
                .baseReels(b -> {
                    b.reel("K", "A", "A", "A", "K", "K");
                    b.reel("K", "A", "A", "A", "K", "K");
                    b.reel("K", "A", "A", "A", "K", "K");
                })
                .build();
        Window window = new Window(new String[][]{
                {"A", "A", "K"},
                {"A", "A", "K"},
                {"A", "A", "K"}
        }, new int[]{1, 1, 1});
        Win win = Win.line("A", 3, 0, 1, 1, List.of(
                new Position(0, 0), new Position(1, 0), new Position(2, 0)
        ));
        int[] pulled = new int[3];
        Window next = new CascadeProcessor().drop(game, game.baseReels(), window, List.of(win), pulled);
        assertThat(next.at(0, 2)).isEqualTo("K");
        assertThat(next.at(0, 1)).isEqualTo("A");
        assertThat(pulled[0]).isEqualTo(1);
    }
}
