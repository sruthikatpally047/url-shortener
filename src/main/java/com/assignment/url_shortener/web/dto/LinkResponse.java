package com.assignment.url_shortener.web.dto;

import java.time.Instant;

public record LinkResponse(String code, String shortUrl, String originalUrl, Instant createdAt,
        Instant expiresAt, LinkStatus status) {
    public enum LinkStatus { ACTIVE, EXPIRED, DISABLED }
}
