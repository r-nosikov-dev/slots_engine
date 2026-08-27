package com.slotengine.api.wallet;

import com.slotengine.api.ledger.InsufficientFundsException;
import com.slotengine.api.ledger.WalletContext;
import com.slotengine.api.ledger.WalletReceipt;
import com.slotengine.model.PlayMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedWalletGatewayTest {

    @Test
    void debitCreditAndIdempotentRetry() {
        SimulatedWalletGateway wallet = new SimulatedWalletGateway();
        wallet.seed("p", 100);
        WalletContext bet = ctx("r1", "r1:bet");
        WalletReceipt first = wallet.debitBet(bet, 20);
        assertThat(first.balanceAfter()).isEqualTo(80);
        assertThat(first.duplicate()).isFalse();
        WalletReceipt retry = wallet.debitBet(bet, 20);
        assertThat(retry.duplicate()).isTrue();
        assertThat(wallet.balance("p")).isEqualTo(80);

        WalletReceipt win = wallet.creditWin(ctx("r1", "r1:win"), 50);
        assertThat(win.balanceAfter()).isEqualTo(130);
        assertThat(wallet.creditWin(ctx("r1", "r1:win"), 50).duplicate()).isTrue();
        assertThat(wallet.balance("p")).isEqualTo(130);
    }

    @Test
    void rollbackRestoresBet() {
        SimulatedWalletGateway wallet = new SimulatedWalletGateway();
        wallet.seed("p", 40);
        wallet.debitBet(ctx("r2", "r2:bet"), 40);
        wallet.rollbackBet(ctx("r2", "r2:rollback"));
        assertThat(wallet.balance("p")).isEqualTo(40);
        wallet.rollbackBet(ctx("r2", "r2:rollback"));
        assertThat(wallet.balance("p")).isEqualTo(40);
    }

    @Test
    void insufficientFunds() {
        SimulatedWalletGateway wallet = new SimulatedWalletGateway();
        wallet.seed("p", 5);
        assertThatThrownBy(() -> wallet.debitBet(ctx("r3", "r3:bet"), 10))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(wallet.balance("p")).isEqualTo(5);
    }

    private static WalletContext ctx(String roundId, String txId) {
        return new WalletContext("p", "s", "classic-fruits", roundId, txId, "CREDITS", PlayMode.NORMAL);
    }
}
