package com.slotengine.api.session;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SessionStore {

    private final ConcurrentMap<String, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSession create(String playerId, String gameId) {
        GameSession session = new GameSession(UUID.randomUUID().toString(), playerId, gameId);
        sessions.put(session.id(), session);
        return session;
    }

    public Optional<GameSession> find(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public GameSession require(String id) {
        return find(id).orElseThrow(() -> new SessionNotFoundException(id));
    }

    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String id) {
            super("Session not found: " + id);
        }
    }
}
