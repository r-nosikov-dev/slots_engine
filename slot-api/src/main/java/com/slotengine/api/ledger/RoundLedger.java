package com.slotengine.api.ledger;

import com.slotengine.engine.RoundResult;

import java.util.Optional;

/**
 * Durable (later: DB) store for round settlement. In-memory today; swap the bean
 * for a JDBC implementation without touching {@code PlayService}.
 */
public interface RoundLedger {

    Optional<SettledRound> findSettled(String roundId);

    void saveResult(RoundResult result);

    Optional<RoundResult> findResult(String roundId);

    void markSettled(SettledRound settled);
}
