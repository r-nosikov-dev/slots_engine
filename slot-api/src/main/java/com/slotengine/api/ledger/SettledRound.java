package com.slotengine.api.ledger;

import com.slotengine.engine.RoundResult;

public record SettledRound(
        RoundResult result,
        WalletReceipt bet,
        WalletReceipt win
) {
}
