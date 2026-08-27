package com.slotengine.api.wallet;

import com.slotengine.api.config.SlotProperties;
import com.slotengine.api.ledger.WalletContext;
import com.slotengine.api.ledger.WalletReceipt;
import com.slotengine.model.PlayMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class HttpOperatorWalletGatewayTest {

    private MockRestServiceServer server;
    private HttpOperatorWalletGateway gateway;

    @BeforeEach
    void setUp() {
        RestTemplate template = new RestTemplate();
        server = MockRestServiceServer.bindTo(template).build();
        SlotProperties.HttpWallet http = new SlotProperties.HttpWallet();
        http.setBaseUrl("http://operator.example");
        RestClient client = RestClient.builder(template).baseUrl(http.getBaseUrl()).build();
        gateway = new HttpOperatorWalletGateway(client, http, "CREDITS");
    }

    @Test
    void debitPostsOperatorContract() {
        server.expect(requestTo("http://operator.example/wallet/debit"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.playerId").value("p"))
                .andExpect(jsonPath("$.roundId").value("r1"))
                .andExpect(jsonPath("$.txId").value("r1:bet"))
                .andExpect(jsonPath("$.amount").value(20))
                .andExpect(jsonPath("$.type").value("BET"))
                .andRespond(withSuccess(
                        "{\"txId\":\"r1:bet\",\"providerRef\":\"op-9\",\"balance\":80,\"duplicate\":false}",
                        MediaType.APPLICATION_JSON
                ));

        WalletContext ctx = new WalletContext("p", "s", "classic-fruits", "r1", "r1:bet", "CREDITS", PlayMode.NORMAL);
        WalletReceipt receipt = gateway.debitBet(ctx, 20);
        assertThat(receipt.balanceAfter()).isEqualTo(80);
        assertThat(receipt.providerRef()).isEqualTo("op-9");
        server.verify();
    }
}
