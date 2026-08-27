package com.slotengine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotengine.api.ledger.WalletGateway;
import com.slotengine.api.wallet.SimulatedWalletGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "slot.mode=LIVE",
        "slot.wallet.provider=SIMULATED",
        "slot.math-enabled=true"
})
@AutoConfigureMockMvc
class LiveModeApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    WalletGateway wallet;

    @Autowired
    ObjectMapper mapper;

    @BeforeEach
    void seedPlayer() {
        ((SimulatedWalletGateway) wallet).seed("live-player", 500);
    }

    @Test
    void liveDisablesMathAndTopUpAndClientSeed() throws Exception {
        mvc.perform(get("/api/v1/runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatingMode").value("LIVE"))
                .andExpect(jsonPath("$.mathEnabled").value(false))
                .andExpect(jsonPath("$.allowTopUp").value(false))
                .andExpect(jsonPath("$.allowClientSeed").value(false));

        mvc.perform(post("/api/v1/math/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\":\"classic-fruits\",\"spins\":100}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MATH_DISABLED"));

        mvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"live-player\",\"gameId\":\"classic-fruits\",\"credits\":999}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("TOP_UP_FORBIDDEN"));
    }

    @Test
    void liveSpinDebitsOperatorBalanceWithoutTopUp() throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"live-player\",\"gameId\":\"classic-fruits\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500))
                .andExpect(jsonPath("$.operatingMode").value("LIVE"))
                .andReturn();
        String sessionId = mapper.readTree(created.getResponse().getContentAsString())
                .get("sessionId").asText();

        mvc.perform(post("/api/v1/sessions/" + sessionId + "/spin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bet\":10,\"mode\":\"NORMAL\",\"seed\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CLIENT_SEED_FORBIDDEN"));

        mvc.perform(post("/api/v1/sessions/" + sessionId + "/spin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bet\":10,\"mode\":\"NORMAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.charged").value(10))
                .andExpect(jsonPath("$.operatingMode").value("LIVE"))
                .andExpect(jsonPath("$.betTxId").exists());
    }
}
