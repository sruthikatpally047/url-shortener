package com.assignment.url_shortener.service;

import com.assignment.url_shortener.domain.ShortLink;
import com.assignment.url_shortener.domain.ShortLinkRepository;
import com.assignment.url_shortener.exception.LinkException;
import com.assignment.url_shortener.validation.UrlPolicy;
import com.assignment.url_shortener.web.dto.*;
import com.assignment.url_shortener.web.dto.LinkResponse.LinkStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ShortLinkService {
    private final ShortLinkRepository repository;
    private final LinkWriter writer;
    private final CodeGenerator generator;
    private final UrlPolicy urlPolicy;
    private final Clock clock;
    private final String baseUrl;

    public ShortLinkService(ShortLinkRepository repository, LinkWriter writer, CodeGenerator generator,
            UrlPolicy urlPolicy, Clock clock, @Value("${app.base-url}") String baseUrl) {
        this.repository = repository;
        this.writer = writer;
        this.generator = generator;
        this.urlPolicy = urlPolicy;
        this.clock = clock;
        urlPolicy.validate(baseUrl);
        URI base = URI.create(baseUrl);
        if (base.getRawQuery() != null || base.getRawFragment() != null) {
            throw new IllegalArgumentException("app.base-url must not contain a query or fragment");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public LinkResponse create(CreateLinkRequest request) {
        urlPolicy.validate(request.originalUrl());
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Instant expiresAt = request.expiresAt() == null ? null : request.expiresAt().truncatedTo(ChronoUnit.MICROS);
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new LinkException(HttpStatus.BAD_REQUEST, "expiresAt must be in the future (microsecond precision)");
        }
        boolean custom = request.customAlias() != null;
        // Keep domain invariants for direct callers as well as HTTP bean validation.
        if (custom && !request.customAlias().matches("[A-Za-z0-9_-]{4,32}")) {
            throw new LinkException(HttpStatus.BAD_REQUEST, "customAlias must contain 4-32 letters, digits, hyphens or underscores");
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = custom ? request.customAlias() : generator.generate();
            try {
                return response(writer.insert(code, request.originalUrl(), now, expiresAt), now);
            } catch (DataIntegrityViolationException exception) {
                // Retry only a code conflict, never mask an unrelated database constraint failure.
                if (repository.findByCode(code).isEmpty()) throw exception;
                if (custom) throw new LinkException(HttpStatus.CONFLICT, "This custom alias is already in use");
            }
        }
        throw new LinkException(HttpStatus.SERVICE_UNAVAILABLE, "Could not allocate a short code; please retry");
    }

    @Transactional(readOnly = true)
    public LinkResponse get(String code) { return response(find(code), clock.instant()); }

    @Transactional
    public String resolve(String code) {
        Instant now = clock.instant();
        if (repository.recordClick(code, now) == 0) {
            requireActive(find(code), now);
            throw new LinkException(HttpStatus.SERVICE_UNAVAILABLE, "Link changed during redirect; please retry");
        }
        return find(code).getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public String preview(String code) {
        ShortLink link = find(code);
        requireActive(link, clock.instant());
        return link.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(String code) {
        ShortLink link = find(code);
        return new AnalyticsResponse(code, link.getClickCount(), link.getLastClickedAt());
    }

    @Transactional
    public void disable(String code) {
        if (repository.disable(code) == 0) throw notFound();
    }

    private ShortLink find(String code) { return repository.findByCode(code).orElseThrow(this::notFound); }

    private LinkException notFound() { return new LinkException(HttpStatus.NOT_FOUND, "Short link not found"); }

    private void requireActive(ShortLink link, Instant now) {
        LinkStatus status = status(link, now);
        if (status != LinkStatus.ACTIVE) {
            throw new LinkException(HttpStatus.GONE, status == LinkStatus.DISABLED ? "Short link is disabled" : "Short link has expired");
        }
    }

    private LinkStatus status(ShortLink link, Instant now) {
        if (!link.isEnabled()) return LinkStatus.DISABLED;
        if (link.getExpiresAt() != null && !link.getExpiresAt().isAfter(now)) return LinkStatus.EXPIRED;
        return LinkStatus.ACTIVE;
    }

    private LinkResponse response(ShortLink link, Instant now) {
        return new LinkResponse(link.getCode(), baseUrl + "/r/" + link.getCode(), link.getOriginalUrl(),
                link.getCreatedAt(), link.getExpiresAt(), status(link, now));
    }
}
