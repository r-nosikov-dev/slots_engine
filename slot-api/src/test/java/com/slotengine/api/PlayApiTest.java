package com.slotengine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PlayApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    void listGamesAndSpin() throws Exception {
        mvc.perform(get("/api/v1/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isString());

        mvc.perform(get("/api/v1/runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatingMode").value("SIMULATION"))
                .andExpect(jsonPath("$.mathEnabled").value(true))
                .andExpect(jsonPath("$.allowTopUp").value(true));

        MvcResult created = mvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"p1\",\"gameId\":\"classic-fruits\",\"credits\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000))
                .andExpect(jsonPath("$.operatingMode").value("SIMULATION"))
                .andReturn();
        String sessionId = mapper.readTree(created.getResponse().getContentAsString()).get("sessionId").asText();

        MvcResult spun = mvc.perform(post("/api/v1/sessions/" + sessionId + "/spin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bet\":10,\"mode\":\"NORMAL\",\"seed\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundId").isString())
                .andExpect(jsonPath("$.spins[0].window").isArray())
                .andExpect(jsonPath("$.betTxId").value(org.hamcrest.Matchers.endsWith(":bet")))
                .andReturn();
        JsonNode round = mapper.readTree(spun.getResponse().getContentAsString());
        assertThat(round.get("charged").asLong()).isEqualTo(10);
        assertThat(round.get("balance").asLong()).isEqualTo(1000 - 10 + round.get("totalWin").asLong());

        String roundId = round.get("roundId").asText();
        MvcResult replayed = mvc.perform(post("/api/v1/sessions/" + sessionId + "/spin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", roundId)
                        .content("{\"bet\":10,\"mode\":\"NORMAL\",\"seed\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode again = mapper.readTree(replayed.getResponse().getContentAsString());
        assertThat(again.get("roundId").asText()).isEqualTo(roundId);
        assertThat(again.get("totalWin").asLong()).isEqualTo(round.get("totalWin").asLong());
        assertThat(again.get("balance").asLong()).isEqualTo(round.get("balance").asLong());
    }

    @Test
    void unknownGameIs400() throws Exception {
        mvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"p1\",\"gameId\":\"nope\"}"))
                .andExpect(status().isBadRequest());
    }
}
