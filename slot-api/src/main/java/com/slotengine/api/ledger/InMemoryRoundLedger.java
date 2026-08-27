package com.slotengine.api.ledger;

import com.slotengine.engine.RoundResult;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryRoundLedger implements RoundLedger {

    private final ConcurrentMap<String, RoundResult> results = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SettledRound> settled = new ConcurrentHashMap<>();

    @Override
    public Optional<SettledRound> findSettled(String roundId) {
        return Optional.ofNullable(settled.get(roundId));
    }

    @Override
    public void saveResult(RoundResult result) {
        results.put(result.roundId(), result);
    }

    @Override
    public Optional<RoundResult> findResult(String roundId) {
        return Optional.ofNullable(results.get(roundId));
    }

    @Override
    public void markSettled(SettledRound settledRound) {
        results.put(settledRound.result().roundId(), settledRound.result());
        settled.put(settledRound.result().roundId(), settledRound);
    }
}
