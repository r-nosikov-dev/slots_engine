package com.slotengine.api.ledger;

public class InsufficientFundsException extends WalletException {

    private final long balance;
    private final long required;

    public InsufficientFundsException(long balance, long required) {
        super("Insufficient funds: have " + balance + ", need " + required);
        this.balance = balance;
        this.required = required;
    }

    public long balance() {
        return balance;
    }

    public long required() {
        return required;
    }
}
