package com.slotengine.api.web;

import com.slotengine.api.dto.ApiDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Play", description = "Sessions, spins, replay")
public class PlayController {

    private final PlayService playService;

    public PlayController(PlayService playService) {
        this.playService = playService;
    }

    @PostMapping("/sessions")
    @Operation(summary = "Open a play session. credits is ignored in LIVE — balance comes from the operator wallet.")
    public ApiDtos.SessionResponse create(@Valid @RequestBody ApiDtos.CreateSessionRequest request) {
        return playService.createSession(request);
    }

    @GetMapping("/sessions/{id}")
    public ApiDtos.SessionResponse get(@PathVariable String id) {
        return playService.getSession(id);
    }

    @PostMapping("/sessions/{id}/spin")
    @Operation(summary = "Play one round. Debit → engine → credit. Idempotency-Key / roundId retries settlement, not the spin.")
    public ApiDtos.RoundResponse spin(
            @PathVariable String id,
            @Valid @RequestBody ApiDtos.SpinRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return playService.spin(id, request, idempotencyKey);
    }

    @GetMapping("/rounds/{roundId}")
    @Operation(summary = "Replay a stored round by id")
    public ApiDtos.RoundResponse round(@PathVariable String roundId) {
        return playService.replay(roundId);
    }
}
