package com.slotengine.api.ledger;

import com.slotengine.model.PlayMode;

/**
 * Correlation data sent with every wallet call. {@code txId} is the idempotency key:
 * retrying the same {@code (type, txId)} must not move money twice.
 */
public record WalletContext(
        String playerId,
        String sessionId,
        String gameId,
        String roundId,
        String txId,
        String currency,
        PlayMode playMode
) {

    public WalletContext {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId is required");
        }
        if (roundId == null || roundId.isBlank()) {
            throw new IllegalArgumentException("roundId is required");
        }
        if (txId == null || txId.isBlank()) {
            throw new IllegalArgumentException("txId is required");
        }
        if (currency == null || currency.isBlank()) {
            currency = "CREDITS";
        }
        if (playMode == null) {
            playMode = PlayMode.NORMAL;
        }
    }

    public WalletContext withTxId(String nextTxId) {
        return new WalletContext(playerId, sessionId, gameId, roundId, nextTxId, currency, playMode);
    }

    public static String betTxId(String roundId) {
        return roundId + ":bet";
    }

    public static String winTxId(String roundId) {
        return roundId + ":win";
    }

    public static String rollbackTxId(String roundId) {
        return roundId + ":rollback";
    }
}
