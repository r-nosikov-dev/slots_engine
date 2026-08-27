package com.slotengine.api.wallet;

import com.slotengine.api.ledger.InsufficientFundsException;
import com.slotengine.api.ledger.MoneyTxType;
import com.slotengine.api.ledger.WalletContext;
import com.slotengine.api.ledger.WalletException;
import com.slotengine.api.ledger.WalletGateway;
import com.slotengine.api.ledger.WalletProvider;
import com.slotengine.api.ledger.WalletReceipt;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-process ledger used in {@code SIMULATION} and in LIVE integration tests.
 * Idempotent on {@code txId}: a retry returns the original receipt and does not move money again.
 */
public final class SimulatedWalletGateway implements WalletGateway {

    private final ConcurrentMap<String, Long> balances = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WalletReceipt> receipts = new ConcurrentHashMap<>();

    @Override
    public WalletProvider provider() {
        return WalletProvider.SIMULATED;
    }

    @Override
    public long balance(String playerId) {
        return balances.getOrDefault(playerId, 0L);
    }

    @Override
    public WalletReceipt debitBet(WalletContext context, long amount) {
        return mutate(context, MoneyTxType.BET, amount, true);
    }

    @Override
    public WalletReceipt creditWin(WalletContext context, long amount) {
        if (amount == 0) {
            WalletReceipt zero = WalletReceipt.accepted(
                    context.txId(), context.roundId(), MoneyTxType.WIN, 0, balance(context.playerId()), "sim-zero"
            );
            WalletReceipt raced = receipts.putIfAbsent(context.txId(), zero);
            return raced == null ? zero : WalletReceipt.duplicate(raced);
        }
        return mutate(context, MoneyTxType.WIN, amount, false);
    }

    @Override
    public WalletReceipt rollbackBet(WalletContext context) {
        WalletReceipt bet = receipts.get(WalletContext.betTxId(context.roundId()));
        if (bet == null) {
            return WalletReceipt.accepted(
                    context.txId(), context.roundId(), MoneyTxType.ROLLBACK, 0, balance(context.playerId()), "sim-void"
            );
        }
        return mutate(context.withTxId(WalletContext.rollbackTxId(context.roundId())), MoneyTxType.ROLLBACK, bet.amount(), false);
    }

    @Override
    public WalletReceipt topUp(String playerId, long amount) {
        requirePositive(amount);
        long next = balances.merge(playerId, amount, Long::sum);
        return WalletReceipt.accepted("topup-" + playerId + "-" + next, "topup", MoneyTxType.TOP_UP, amount, next, "sim");
    }

    /** Test/studio helper: set an absolute balance. */
    public void seed(String playerId, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("seed must be >= 0");
        }
        balances.put(playerId, amount);
    }

    private WalletReceipt mutate(WalletContext context, MoneyTxType type, long amount, boolean debit) {
        requirePositive(amount);
        WalletReceipt existing = receipts.get(context.txId());
        if (existing != null) {
            return WalletReceipt.duplicate(existing);
        }
        while (true) {
            long current = balances.getOrDefault(context.playerId(), 0L);
            long next = debit ? current - amount : current + amount;
            if (debit && current < amount) {
                throw new InsufficientFundsException(current, amount);
            }
            Long previous = balances.putIfAbsent(context.playerId(), next);
            if (previous == null) {
                if (debit && current != 0) {
                    continue;
                }
                return store(context, type, amount, next);
            }
            if (previous != current) {
                continue;
            }
            if (balances.replace(context.playerId(), previous, next)) {
                return store(context, type, amount, next);
            }
        }
    }

    private WalletReceipt store(WalletContext context, MoneyTxType type, long amount, long balanceAfter) {
        WalletReceipt receipt = WalletReceipt.accepted(
                context.txId(), context.roundId(), type, amount, balanceAfter, "sim-" + context.txId()
        );
        WalletReceipt raced = receipts.putIfAbsent(context.txId(), receipt);
        return raced == null ? receipt : WalletReceipt.duplicate(raced);
    }

    private static void requirePositive(long amount) {
        if (amount < 0) {
            throw new WalletException("amount must be >= 0");
        }
        if (amount == 0) {
            throw new WalletException("amount must be > 0");
        }
    }
}
