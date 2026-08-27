package com.slotengine.api.dto;

import com.slotengine.engine.FeatureTrigger;
import com.slotengine.engine.RoundResult;
import com.slotengine.engine.SpinSnapshot;
import com.slotengine.engine.eval.Win;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.PlayMode;
import com.slotengine.model.Position;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record GameSummary(
            String id,
            String name,
            String version,
            String theme,
            String grid,
            String evaluation,
            int paylines,
            Integer ways,
            String targetRtp,
            String volatility,
            String maxWin,
            boolean freeSpins,
            boolean buyBonus,
            boolean cascade
    ) {
        public static GameSummary from(GameDefinition game) {
            Map<String, Object> s = game.summary();
            return new GameSummary(
                    game.id(),
                    game.name(),
                    game.version(),
                    game.theme(),
                    game.grid().toString(),
                    game.evaluation().name(),
                    game.lineCount(),
                    game.evaluation().name().equals("WAYS") ? game.grid().ways() : null,
                    game.math().targetRtp().toPlainString(),
                    game.math().volatility().name(),
                    game.math().maxWinMultiplier() + "x",
                    game.features().freeSpins().isPresent(),
                    game.features().buyBonus().isPresent(),
                    (Boolean) s.get("cascade")
            );
        }
    }

    public record CreateSessionRequest(
            @NotBlank String playerId,
            @NotBlank String gameId,
            Long credits
    ) {
    }

    public record SessionResponse(
            String sessionId,
            String playerId,
            String gameId,
            long balance,
            String lastRoundId,
            String operatingMode,
            String currency
    ) {
    }

    public record SpinRequest(
            @NotNull @Min(1) Long bet,
            PlayMode mode,
            Long seed,
            String roundId
    ) {
    }

    public record PositionDto(int reel, int row) {
        static PositionDto from(Position p) {
            return new PositionDto(p.reel(), p.row());
        }
    }

    public record WinDto(
            String type,
            String symbolId,
            int ofAKind,
            int waysOrLine,
            long amount,
            int multiplier,
            List<PositionDto> positions
    ) {
        static WinDto from(Win win) {
            return new WinDto(
                    win.type().name(),
                    win.symbolId(),
                    win.ofAKind(),
                    win.waysOrLine(),
                    win.amount(),
                    win.multiplier(),
                    win.positions().stream().map(PositionDto::from).toList()
            );
        }
    }

    public record TriggerDto(String type, String symbolId, int count, int awardedSpins, List<String> details) {
        static TriggerDto from(FeatureTrigger trigger) {
            return new TriggerDto(trigger.type(), trigger.symbolId(), trigger.count(), trigger.awardedSpins(), trigger.details());
        }
    }

    public record SpinDto(
            int index,
            String phase,
            List<List<String>> window,
            int[] stops,
            List<WinDto> wins,
            long winAmount,
            int multiplier,
            List<TriggerDto> triggers,
            int freeSpinsRemaining
    ) {
        static SpinDto from(SpinSnapshot spin) {
            return new SpinDto(
                    spin.index(),
                    spin.phase().name(),
                    spin.window().rowsFirst(),
                    spin.window().stops(),
                    spin.wins().stream().map(WinDto::from).toList(),
                    spin.winAmount(),
                    spin.multiplier(),
                    spin.triggers().stream().map(TriggerDto::from).toList(),
                    spin.freeSpinsRemaining()
            );
        }
    }

    public record RoundResponse(
            String roundId,
            String gameId,
            String mode,
            long seed,
            long totalBet,
            long charged,
            long totalWin,
            boolean maxWinCapped,
            long balance,
            String operatingMode,
            String betTxId,
            String winTxId,
            String currency,
            List<TriggerDto> triggers,
            List<SpinDto> spins
    ) {
        public static RoundResponse from(
                RoundResult result,
                long balance,
                String operatingMode,
                String betTxId,
                String winTxId,
                String currency
        ) {
            return new RoundResponse(
                    result.roundId(),
                    result.gameId(),
                    result.mode().name(),
                    result.seed(),
                    result.totalBet(),
                    result.charged(),
                    result.totalWin(),
                    result.maxWinCapped(),
                    balance,
                    operatingMode,
                    betTxId,
                    winTxId,
                    currency,
                    result.triggers().stream().map(TriggerDto::from).toList(),
                    result.spins().stream().map(SpinDto::from).toList()
            );
        }
    }

    public record RuntimeResponse(
            String operatingMode,
            String walletProvider,
            boolean mathEnabled,
            boolean studioEnabled,
            boolean allowClientSeed,
            boolean allowTopUp,
            String currency
    ) {
    }

    public record SimulateRequest(
            @NotBlank String gameId,
            @NotNull @Min(1) Long spins,
            Long bet,
            PlayMode mode,
            Long seed
    ) {
    }

    public record EnumerateRequest(
            @NotBlank String gameId,
            Long bet
    ) {
    }

    public record FromTemplateRequest(
            @NotBlank String id,
            @NotBlank String template,
            String name,
            String theme
    ) {
    }

    public record ErrorResponse(String error, String message, List<String> details) {
    }
}
