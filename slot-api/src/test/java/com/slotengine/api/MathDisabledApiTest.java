package com.slotengine.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "slot.math-enabled=false")
@AutoConfigureMockMvc
class MathDisabledApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void mathFlagDisablesSimulationEndpointsWithoutGoingLive() throws Exception {
        mvc.perform(get("/api/v1/runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatingMode").value("SIMULATION"))
                .andExpect(jsonPath("$.mathEnabled").value(false))
                .andExpect(jsonPath("$.allowTopUp").value(true));

        mvc.perform(post("/api/v1/math/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\":\"classic-fruits\",\"spins\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MATH_DISABLED"));
    }
}
