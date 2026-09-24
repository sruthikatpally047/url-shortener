package com.assignment.url_shortener.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record AnalyticsResponse(String code,
        @Schema(description = "Accepted GET redirects, including repeat visits and bots; not unique visitors") long totalClicks,
        Instant lastClickedAt) {}
