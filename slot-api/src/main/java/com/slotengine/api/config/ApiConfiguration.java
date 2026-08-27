package com.slotengine.api.config;

import com.slotengine.SlotsEngine;
import com.slotengine.engine.catalog.GameCatalog;
import com.slotengine.math.BaseGameEnumerator;
import com.slotengine.math.MonteCarloSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
@EnableConfigurationProperties(SlotProperties.class)
public class ApiConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ApiConfiguration.class);

    @Bean
    GameCatalog gameCatalog(SlotProperties properties) throws IOException {
        GameCatalog catalog = GameCatalog.builtin();
        Path dir = resolveGamesDir(properties.getGamesDir());
        if (dir != null) {
            catalog.loadDirectory(dir);
            log.info("Loaded extra games from {}", dir.toAbsolutePath());
        }
        log.info("Catalog: {}", catalog.all().stream().map(g -> g.id()).toList());
        return catalog;
    }

    public static Path resolveGamesDir(String configured) {
        List<Path> candidates = new java.util.ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(Path.of(configured));
        }
        candidates.add(Path.of("games"));
        candidates.add(Path.of("..", "games"));
        for (Path path : candidates) {
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return null;
    }

    @Bean
    SlotsEngine slotsEngine(GameCatalog catalog) {
        return new SlotsEngine(catalog);
    }

    @Bean
    MonteCarloSimulator monteCarloSimulator() {
        return new MonteCarloSimulator();
    }

    @Bean
    BaseGameEnumerator baseGameEnumerator() {
        return new BaseGameEnumerator();
    }

    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
