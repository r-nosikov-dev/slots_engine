package com.slotengine.api.wallet;

import com.slotengine.api.config.SlotProperties;
import com.slotengine.api.ledger.InsufficientFundsException;
import com.slotengine.api.ledger.MoneyTxType;
import com.slotengine.api.ledger.WalletContext;
import com.slotengine.api.ledger.WalletException;
import com.slotengine.api.ledger.WalletGateway;
import com.slotengine.api.ledger.WalletProvider;
import com.slotengine.api.ledger.WalletReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Operator wallet over HTTP. Paths are configurable; bodies follow {@link OperatorWalletModels}.
 *
 * <pre>
 * POST {baseUrl}{debitPath}     → DebitCreditRequest  / WalletResponse
 * POST {baseUrl}{creditPath}    → DebitCreditRequest  / WalletResponse
 * POST {baseUrl}{rollbackPath}  → RollbackRequest     / WalletResponse
 * GET  {baseUrl}{balancePath}   → WalletResponse      (path may contain {playerId})
 * </pre>
 *
 * Map operator {@code 409} + {@code error=INSUFFICIENT_FUNDS} to {@link InsufficientFundsException}.
 */
public final class HttpOperatorWalletGateway implements WalletGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpOperatorWalletGateway.class);

    private final RestClient client;
    private final SlotProperties.HttpWallet http;
    private final String currency;

    public HttpOperatorWalletGateway(RestClient client, SlotProperties.HttpWallet http, String currency) {
        this.client = client;
        this.http = http;
        this.currency = currency;
    }

    @Override
    public WalletProvider provider() {
        return WalletProvider.HTTP;
    }

    @Override
    public long balance(String playerId) {
        String path = http.getBalancePath().replace("{playerId}", playerId);
        OperatorWalletModels.WalletResponse body = get(path);
        return body.balance();
    }

    @Override
    public WalletReceipt debitBet(WalletContext context, long amount) {
        return postMoney(http.getDebitPath(), context, MoneyTxType.BET, amount);
    }

    @Override
    public WalletReceipt creditWin(WalletContext context, long amount) {
        if (amount == 0) {
            return WalletReceipt.accepted(
                    context.txId(), context.roundId(), MoneyTxType.WIN, 0, balance(context.playerId()), "operator-zero"
            );
        }
        return postMoney(http.getCreditPath(), context, MoneyTxType.WIN, amount);
    }

    @Override
    public WalletReceipt rollbackBet(WalletContext context) {
        OperatorWalletModels.RollbackRequest request = new OperatorWalletModels.RollbackRequest(
                context.playerId(),
                context.sessionId(),
                context.gameId(),
                context.roundId(),
                context.txId(),
                WalletContext.betTxId(context.roundId())
        );
        OperatorWalletModels.WalletResponse body = post(http.getRollbackPath(), request);
        return toReceipt(context, MoneyTxType.ROLLBACK, 0, body);
    }

    private WalletReceipt postMoney(String path, WalletContext context, MoneyTxType type, long amount) {
        OperatorWalletModels.DebitCreditRequest request = new OperatorWalletModels.DebitCreditRequest(
                context.playerId(),
                context.sessionId(),
                context.gameId(),
                context.roundId(),
                context.txId(),
                currency,
                context.playMode().name(),
                type.name(),
                amount
        );
        OperatorWalletModels.WalletResponse body = post(path, request);
        return toReceipt(context, type, amount, body);
    }

    private OperatorWalletModels.WalletResponse get(String path) {
        try {
            OperatorWalletModels.WalletResponse body = client.get()
                    .uri(path)
                    .retrieve()
                    .body(OperatorWalletModels.WalletResponse.class);
            return requireBody(body);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        }
    }

    private OperatorWalletModels.WalletResponse post(String path, Object payload) {
        try {
            OperatorWalletModels.WalletResponse body = client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(OperatorWalletModels.WalletResponse.class);
            return requireBody(body);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        }
    }

    private RuntimeException translate(RestClientResponseException ex) {
        String payload = ex.getResponseBodyAsString();
        log.warn("Operator wallet HTTP {}: {}", ex.getStatusCode().value(), payload);
        if (ex.getStatusCode().value() == 409 && payload != null && payload.contains("INSUFFICIENT_FUNDS")) {
            return new InsufficientFundsException(-1, -1);
        }
        return new WalletException("Operator wallet failed: HTTP " + ex.getStatusCode().value(), ex);
    }

    private static OperatorWalletModels.WalletResponse requireBody(OperatorWalletModels.WalletResponse body) {
        if (body == null) {
            throw new WalletException("Operator wallet returned an empty body");
        }
        if (body.error() != null && !body.error().isBlank()) {
            if ("INSUFFICIENT_FUNDS".equals(body.error())) {
                throw new InsufficientFundsException(body.balance(), -1);
            }
            throw new WalletException("Operator wallet error: " + body.error());
        }
        return body;
    }

    private static WalletReceipt toReceipt(
            WalletContext context,
            MoneyTxType type,
            long amount,
            OperatorWalletModels.WalletResponse body
    ) {
        String ref = body.providerRef() == null ? body.txId() : body.providerRef();
        WalletReceipt accepted = WalletReceipt.accepted(
                context.txId(), context.roundId(), type, amount, body.balance(), ref
        );
        return body.duplicate() ? WalletReceipt.duplicate(accepted) : accepted;
    }
}
