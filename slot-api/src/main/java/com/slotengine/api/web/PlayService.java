package com.slotengine.api.web;

import com.slotengine.SlotsEngine;
import com.slotengine.api.config.SlotProperties;
import com.slotengine.api.dto.ApiDtos;
import com.slotengine.api.ledger.RoundLedger;
import com.slotengine.api.ledger.SettledRound;
import com.slotengine.api.ledger.WalletContext;
import com.slotengine.api.ledger.WalletGateway;
import com.slotengine.api.ledger.WalletReceipt;
import com.slotengine.api.session.GameSession;
import com.slotengine.api.session.SessionStore;
import com.slotengine.engine.PlayRequest;
import com.slotengine.engine.RoundResult;
import com.slotengine.engine.StakeCalculator;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.PlayMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Debit → play → credit, with rollback if the engine fails after debit.
 * The result is persisted before the win credit so a crash mid-settlement
 * retries the wallet call and never re-spins the round.
 */
@Service
public class PlayService {

    private static final Logger log = LoggerFactory.getLogger(PlayService.class);

    private final SlotsEngine slotsEngine;
    private final SessionStore sessions;
    private final WalletGateway wallet;
    private final RoundLedger ledger;
    private final SlotProperties properties;

    public PlayService(
            SlotsEngine slotsEngine,
            SessionStore sessions,
            WalletGateway wallet,
            RoundLedger ledger,
            SlotProperties properties
    ) {
        this.slotsEngine = slotsEngine;
        this.sessions = sessions;
        this.wallet = wallet;
        this.ledger = ledger;
        this.properties = properties;
    }

    public ApiDtos.SessionResponse createSession(ApiDtos.CreateSessionRequest request) {
        GameDefinition game = slotsEngine.require(request.gameId());
        if (request.credits() != null && request.credits() > 0 && !properties.topUpAllowed()) {
            throw ForbiddenInLiveException.topUp();
        }
        GameSession session = sessions.create(request.playerId(), game.id());
        if (properties.topUpAllowed()) {
            long credits = request.credits() == null ? properties.getDefaultCredits() : request.credits();
            if (credits > 0) {
                wallet.topUp(session.playerId(), credits);
            }
        }
        return toSession(session);
    }

    public ApiDtos.SessionResponse getSession(String sessionId) {
        return toSession(sessions.require(sessionId));
    }

    public ApiDtos.RoundResponse spin(String sessionId, ApiDtos.SpinRequest request, String idempotencyKey) {
        GameSession session = sessions.require(sessionId);
        GameDefinition game = slotsEngine.require(session.gameId());
        PlayMode mode = request.mode() == null ? PlayMode.NORMAL : request.mode();
        if (request.seed() != null && !properties.clientSeedAllowed()) {
            throw ForbiddenInLiveException.seed();
        }

        String roundId = firstNonBlank(request.roundId(), idempotencyKey, UUID.randomUUID().toString());
        Optional<SettledRound> already = ledger.findSettled(roundId);
        if (already.isPresent()) {
            return toRound(already.get().result(), already.get().bet(), already.get().win(), session.playerId());
        }

        long charged = StakeCalculator.charged(game, request.bet(), mode);
        WalletContext betCtx = context(session, game, roundId, WalletContext.betTxId(roundId), mode);
        WalletReceipt bet = wallet.debitBet(betCtx, charged);

        RoundResult result;
        try {
            result = ledger.findResult(roundId).orElse(null);
            if (result == null) {
                PlayRequest playRequest = new PlayRequest(
                        request.bet(),
                        mode,
                        request.seed() == null ? OptionalLong.empty() : OptionalLong.of(request.seed()),
                        roundId
                );
                result = slotsEngine.play(game, playRequest);
                ledger.saveResult(result);
            }
        } catch (RuntimeException ex) {
            try {
                wallet.rollbackBet(betCtx.withTxId(WalletContext.rollbackTxId(roundId)));
            } catch (RuntimeException rollbackEx) {
                log.error("Rollback failed after engine error roundId={}", roundId, rollbackEx);
            }
            throw ex;
        }

        WalletReceipt win;
        try {
            WalletContext winCtx = betCtx.withTxId(WalletContext.winTxId(roundId));
            win = wallet.creditWin(winCtx, result.totalWin());
        } catch (RuntimeException creditEx) {
            log.error("Win credit failed roundId={} amount={}; result is stored, retry the same roundId",
                    roundId, result.totalWin(), creditEx);
            throw creditEx;
        }

        ledger.markSettled(new SettledRound(result, bet, win));
        session.setLastRoundId(result.roundId());
        return toRound(result, bet, win, session.playerId());
    }

    public ApiDtos.RoundResponse replay(String roundId) {
        SettledRound settled = ledger.findSettled(roundId).orElse(null);
        if (settled != null) {
            return toRound(settled.result(), settled.bet(), settled.win(), null);
        }
        RoundResult result = ledger.findResult(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown round: " + roundId));
        return ApiDtos.RoundResponse.from(result, -1, properties.getMode().name(), null, null, properties.getCurrency());
    }

    private ApiDtos.RoundResponse toRound(RoundResult result, WalletReceipt bet, WalletReceipt win, String playerId) {
        long balance = playerId == null ? (win == null ? -1 : win.balanceAfter()) : wallet.balance(playerId);
        return ApiDtos.RoundResponse.from(
                result,
                balance,
                properties.getMode().name(),
                bet == null ? null : bet.txId(),
                win == null ? null : win.txId(),
                properties.getCurrency()
        );
    }

    private ApiDtos.SessionResponse toSession(GameSession session) {
        return new ApiDtos.SessionResponse(
                session.id(),
                session.playerId(),
                session.gameId(),
                wallet.balance(session.playerId()),
                session.lastRoundId(),
                properties.getMode().name(),
                properties.getCurrency()
        );
    }

    private WalletContext context(
            GameSession session,
            GameDefinition game,
            String roundId,
            String txId,
            PlayMode mode
    ) {
        return new WalletContext(
                session.playerId(),
                session.id(),
                game.id(),
                roundId,
                txId,
                properties.getCurrency(),
                mode
        );
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }
}
