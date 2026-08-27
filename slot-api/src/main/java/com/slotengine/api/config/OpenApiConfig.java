package com.slotengine.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Slot Engine API")
                .version("1.0.0")
                .description("Math core and play protocol for video-slot games. " +
                        "The frontend consumes /spin; a visual editor consumes /games/import."));
    }
}
