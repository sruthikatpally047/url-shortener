package com.assignment.url_shortener.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "short_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShortLink {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 32, updatable = false)
    private String code;
    @Column(name = "original_url", nullable = false, length = 2048, updatable = false)
    private String originalUrl;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", updatable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "click_count", nullable = false)
    private long clickCount;
    @Column(name = "last_clicked_at")
    private Instant lastClickedAt;

    public ShortLink(String code, String originalUrl, Instant createdAt, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.enabled = true;
    }
}
