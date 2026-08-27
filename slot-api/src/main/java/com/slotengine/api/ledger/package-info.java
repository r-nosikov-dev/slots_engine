/**
 * Money port between the math engine and an operator wallet.
 *
 * <p>{@code PlayService} is the only caller: debit bet → {@code SlotEngine.play} →
 * credit win, or rollback the bet if the engine throws. Implement
 * {@link com.slotengine.api.ledger.WalletGateway} (or set
 * {@code slot.wallet.provider=HTTP}) to attach real transactions.
 */
package com.slotengine.api.ledger;
