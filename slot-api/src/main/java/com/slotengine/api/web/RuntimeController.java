package com.slotengine.api.web;

import com.slotengine.api.config.SlotProperties;
import com.slotengine.api.dto.ApiDtos;
import com.slotengine.api.ledger.WalletGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Runtime", description = "Process mode flags for the frontend")
public class RuntimeController {

    private final SlotProperties properties;
    private final WalletGateway wallet;

    public RuntimeController(SlotProperties properties, WalletGateway wallet) {
        this.properties = properties;
        this.wallet = wallet;
    }

    @GetMapping("/runtime")
    @Operation(summary = "Whether math tools, top-up and client seeds are allowed in this process")
    public ApiDtos.RuntimeResponse runtime() {
        return new ApiDtos.RuntimeResponse(
                properties.getMode().name(),
                wallet.provider().name(),
                properties.mathApiEnabled(),
                properties.studioApiEnabled(),
                properties.clientSeedAllowed(),
                properties.topUpAllowed(),
                properties.getCurrency()
        );
    }
}
