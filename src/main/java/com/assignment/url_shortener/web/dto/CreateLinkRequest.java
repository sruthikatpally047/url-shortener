package com.assignment.url_shortener.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank @Size(max = 2048)
        @Schema(example = "https://example.com/articles/spring-boot") String originalUrl,
        @Pattern(regexp = "[A-Za-z0-9_-]{4,32}", message = "must contain 4-32 letters, digits, hyphens or underscores")
        @Schema(description = "Optional case-sensitive alias; never reused", example = "spring-guide") String customAlias,
        @Schema(description = "Optional future UTC expiry. The link expires exactly at this instant.",
                example = "2030-12-31T23:59:59Z") Instant expiresAt) {}
