package com.slotengine.api.ledger;

public enum WalletProvider {
    /** In-process ledger. Studio and integration tests. */
    SIMULATED,
    /** HTTP operator wallet (SoftSwiss-style debit/credit/rollback). */
    HTTP
}
