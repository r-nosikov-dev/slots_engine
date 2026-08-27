package com.slotengine.model;

/**
 * Classification of a slot symbol. Kind drives evaluation rules:
 * wilds substitute, scatters pay anywhere, bonus/jackpot are special triggers.
 */
public enum SymbolKind {
    REGULAR,
    WILD,
    SCATTER,
    BONUS,
    JACKPOT
}
