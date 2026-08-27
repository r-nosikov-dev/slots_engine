package com.slotengine.api.wallet;

/**
 * Wire contract of {@link HttpOperatorWalletGateway}. Point {@code slot.wallet.http.base-url}
 * at the operator, or reimplement {@link com.slotengine.api.ledger.WalletGateway}.
 */
public final class OperatorWalletModels {

    private OperatorWalletModels() {
    }

    public record DebitCreditRequest(
            String playerId,
            String sessionId,
            String gameId,
            String roundId,
            String txId,
            String currency,
            String playMode,
            String type,
            long amount
    ) {
    }

    public record RollbackRequest(
            String playerId,
            String sessionId,
            String gameId,
            String roundId,
            String txId,
            String originalTxId
    ) {
    }

    public record WalletResponse(
            String txId,
            String providerRef,
            long balance,
            boolean duplicate,
            String error
    ) {
    }
}
