package com.slotengine.model;

/** What a paytable multiplier is applied to. */
public enum PayUnit {
    /** Typical for payline games: multiplier × (totalBet / lineCount). */
    LINE_BET,
    /** Typical for scatters and ways games: multiplier × totalBet. */
    TOTAL_BET
}
