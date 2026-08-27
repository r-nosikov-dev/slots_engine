package com.slotengine.api.config;

import com.slotengine.api.ledger.WalletGateway;
import com.slotengine.api.ledger.WalletProvider;
import com.slotengine.api.wallet.HttpOperatorWalletGateway;
import com.slotengine.api.wallet.SimulatedWalletGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class WalletConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WalletConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(WalletGateway.class)
    WalletGateway walletGateway(SlotProperties properties) {
        WalletProvider provider = properties.getWallet().getProvider();
        if (provider == WalletProvider.HTTP) {
            SlotProperties.HttpWallet http = properties.getWallet().getHttp();
            if (http.getBaseUrl() == null || http.getBaseUrl().isBlank()) {
                throw new IllegalStateException(
                        "slot.wallet.provider=HTTP requires slot.wallet.http.base-url"
                );
            }
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(http.getTimeoutMs());
            factory.setReadTimeout(http.getTimeoutMs());
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl(http.getBaseUrl())
                    .requestFactory(factory);
            if (http.getAuthToken() != null && !http.getAuthToken().isBlank()) {
                builder.defaultHeader(http.getAuthHeader(), http.getAuthToken());
            }
            log.info("Wallet gateway: HTTP {}", http.getBaseUrl());
            return new HttpOperatorWalletGateway(builder.build(), http, properties.getCurrency());
        }
        log.info("Wallet gateway: SIMULATED");
        return new SimulatedWalletGateway();
    }

    @Bean
    ApplicationRunner walletModeGuard(SlotProperties properties, WalletGateway gateway) {
        return args -> log.info(
                "Operating mode={} mathApi={} studio={} clientSeed={} topUp={} wallet={}",
                properties.getMode(),
                properties.mathApiEnabled(),
                properties.studioApiEnabled(),
                properties.clientSeedAllowed(),
                properties.topUpAllowed(),
                gateway.provider()
        );
    }
}
