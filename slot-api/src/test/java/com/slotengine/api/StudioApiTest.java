package com.slotengine.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudioApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void templatesFromTemplateAndMathPatch() throws Exception {
        mvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isString());

        mvc.perform(post("/api/v1/games/from-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"studio-ways\",\"template\":\"ways-243\",\"name\":\"Studio Ways\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("studio-ways"))
                .andExpect(jsonPath("$.evaluation").value("WAYS"));

        mvc.perform(get("/api/v1/games/studio-ways/math"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.core.freeSpinsMultiplier").exists())
                .andExpect(jsonPath("$.baseWeights").isArray());

        mvc.perform(put("/api/v1/games/studio-ways/math")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"core\":{\"freeSpinsMultiplier\":5,\"maxWinMultiplier\":1500},\"math\":{\"targetRtp\":0.93,\"volatility\":\"HIGH\",\"maxWinMultiplier\":1500}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.core.freeSpinsMultiplier").value(5))
                .andExpect(jsonPath("$.math.maxWinMultiplier").value(1500));

        mvc.perform(post("/api/v1/games/studio-ways/math/preview?spins=200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"core\":{\"freeSpinsMultiplier\":1}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(false))
                .andExpect(jsonPath("$.math.core.freeSpinsMultiplier").value(1))
                .andExpect(jsonPath("$.simulation.rtp").exists());

        mvc.perform(get("/api/v1/games/studio-ways/math"))
                .andExpect(jsonPath("$.core.freeSpinsMultiplier").value(5));
    }
}
