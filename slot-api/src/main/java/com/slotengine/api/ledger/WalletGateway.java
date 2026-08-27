package com.slotengine.api.ledger;

/**
 * Port to real money. The math engine never calls this — {@code PlayService} does,
 * in a debit → play → credit (or rollback) loop with idempotent tx ids.
 *
 * <p>To plug an operator wallet: implement this interface and expose it as a Spring
 * {@code @Bean}. The HTTP adapter is the built-in option; a native SDK (SoftSwiss,
 * EveryMatrix, custom) is a second implementation of the same port.
 */
public interface WalletGateway {

    WalletProvider provider();

    long balance(String playerId);

    WalletReceipt debitBet(WalletContext context, long amount);

    WalletReceipt creditWin(WalletContext context, long amount);

    WalletReceipt rollbackBet(WalletContext context);

    /**
     * Studio-only. Live adapters must throw {@link WalletException}.
     */
    default WalletReceipt topUp(String playerId, long amount) {
        throw new WalletException("top-up is not supported by " + provider());
    }
}
