package com.slotengine.engine;

import com.slotengine.model.PlayMode;

import java.util.OptionalLong;

public record PlayRequest(
        long totalBet,
        PlayMode mode,
        OptionalLong seed,
        String roundId
) {

    public PlayRequest {
        if (totalBet <= 0) {
            throw new IllegalArgumentException("totalBet must be positive");
        }
        if (mode == null) {
            mode = PlayMode.NORMAL;
        }
        if (seed == null) {
            seed = OptionalLong.empty();
        }
        if (roundId != null && roundId.isBlank()) {
            roundId = null;
        }
    }

    public PlayRequest(long totalBet, PlayMode mode, OptionalLong seed) {
        this(totalBet, mode, seed, null);
    }

    public static PlayRequest spin(long totalBet) {
        return new PlayRequest(totalBet, PlayMode.NORMAL, OptionalLong.empty(), null);
    }

    public static PlayRequest spin(long totalBet, long seed) {
        return new PlayRequest(totalBet, PlayMode.NORMAL, OptionalLong.of(seed), null);
    }

    public static PlayRequest buyBonus(long totalBet) {
        return new PlayRequest(totalBet, PlayMode.BUY_BONUS, OptionalLong.empty(), null);
    }

    public static PlayRequest ante(long totalBet) {
        return new PlayRequest(totalBet, PlayMode.ANTE, OptionalLong.empty(), null);
    }

    public PlayRequest withRoundId(String id) {
        return new PlayRequest(totalBet, mode, seed, id);
    }
}
