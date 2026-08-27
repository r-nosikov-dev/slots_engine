package com.slotengine.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameBuilderTest {

    @Test
    void rejectsUnknownSymbolOnStrip() {
        assertThatThrownBy(() -> GameDefinition.builder("bad")
                .grid(5, 3)
                .symbol("A")
                .standard10Paylines()
                .linePay("A", 3, 1)
                .baseReels(b -> {
                    b.reel("A", "A", "A", "A");
                    b.reel("A", "A", "A", "A");
                    b.reel("A", "A", "A", "A");
                    b.reel("A", "A", "A", "A");
                    b.reel("A", "A", "X", "A");
                })
                .build()
        ).isInstanceOf(GameValidationException.class)
                .hasMessageContaining("unknown symbol X");
    }

    @Test
    void rejectsPaylinesWithoutLines() {
        assertThatThrownBy(() -> GameDefinition.builder("bad")
                .grid(3, 3)
                .symbol("A")
                .linePay("A", 3, 1)
                .baseReels(b -> {
                    b.reel("A", "A", "A");
                    b.reel("A", "A", "A");
                    b.reel("A", "A", "A");
                })
                .build()
        ).isInstanceOf(GameValidationException.class);
    }

    @Test
    void waysGameDoesNotRequirePaylines() {
        GameDefinition game = GameDefinition.builder("ways")
                .grid(3, 3)
                .ways()
                .symbol("A")
                .linePay("A", 3, 1)
                .baseReels(b -> {
                    b.reel("A", "A", "A");
                    b.reel("A", "A", "A");
                    b.reel("A", "A", "A");
                })
                .build();
        assertThat(game.evaluation()).isEqualTo(EvaluationMode.WAYS);
        assertThat(game.grid().ways()).isEqualTo(27);
    }

    @Test
    void wildCannotReplaceScatter() {
        GameDefinition game = GameDefinition.builder("w")
                .grid(3, 3)
                .ways()
                .wild("W")
                .scatter("S")
                .symbol("A")
                .linePay("A", 3, 1)
                .baseReels(b -> {
                    b.reel("A", "W", "S");
                    b.reel("A", "W", "S");
                    b.reel("A", "W", "S");
                })
                .build();
        assertThat(game.symbol("W").substitutesFor(game.symbol("S"))).isFalse();
        assertThat(game.symbol("W").substitutesFor(game.symbol("A"))).isTrue();
    }
}
