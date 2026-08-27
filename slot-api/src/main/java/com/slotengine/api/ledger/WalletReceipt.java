package com.slotengine.api.ledger;

public record WalletReceipt(
        String txId,
        String roundId,
        MoneyTxType type,
        MoneyTxStatus status,
        long amount,
        long balanceAfter,
        boolean duplicate,
        String providerRef
) {

    public static WalletReceipt accepted(
            String txId,
            String roundId,
            MoneyTxType type,
            long amount,
            long balanceAfter,
            String providerRef
    ) {
        return new WalletReceipt(txId, roundId, type, MoneyTxStatus.ACCEPTED, amount, balanceAfter, false, providerRef);
    }

    public static WalletReceipt duplicate(WalletReceipt original) {
        return new WalletReceipt(
                original.txId(),
                original.roundId(),
                original.type(),
                MoneyTxStatus.DUPLICATE,
                original.amount(),
                original.balanceAfter(),
                true,
                original.providerRef()
        );
    }
}
