package com.slotengine.api.ledger;

public class WalletException extends RuntimeException {

    public WalletException(String message) {
        super(message);
    }

    public WalletException(String message, Throwable cause) {
        super(message, cause);
    }
}
