package com.slotengine.api.ledger;

/**
 * Process role. {@link #SIMULATION} is the math studio: in-memory credits, RTP tools, client seeds.
 * {@link #LIVE} is real money: operator wallet, no math endpoints, no top-up, no client-supplied RNG seed.
 */
public enum OperatingMode {
    SIMULATION,
    LIVE
}
