package com.assignment.url_shortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class ApplicationConfig {
    @Bean
    Clock clock() { return Clock.systemUTC(); }

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("URL Shortener API").version("v1")
                .description("Create short links, follow redirects, and inspect aggregate analytics. "
                        + "Demo management endpoints are unauthenticated; deploy behind access controls."));
    }
}
