package com.assignment.url_shortener.service;

import com.assignment.url_shortener.domain.ShortLink;
import com.assignment.url_shortener.domain.ShortLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LinkWriter {
    private final ShortLinkRepository repository;

    // A failed unique constraint rolls back only this attempt, allowing a safe retry.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ShortLink insert(String code, String url, Instant createdAt, Instant expiresAt) {
        return repository.saveAndFlush(new ShortLink(code, url, createdAt, expiresAt));
    }
}
