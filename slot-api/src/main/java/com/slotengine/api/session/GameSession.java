package com.slotengine.api.session;

import java.time.Instant;

public final class GameSession {

    private final String id;
    private final String playerId;
    private final String gameId;
    private final Instant createdAt;
    private volatile String lastRoundId;

    public GameSession(String id, String playerId, String gameId) {
        this.id = id;
        this.playerId = playerId;
        this.gameId = gameId;
        this.createdAt = Instant.now();
    }

    public String id() {
        return id;
    }

    public String playerId() {
        return playerId;
    }

    public String gameId() {
        return gameId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String lastRoundId() {
        return lastRoundId;
    }

    public void setLastRoundId(String lastRoundId) {
        this.lastRoundId = lastRoundId;
    }
}
