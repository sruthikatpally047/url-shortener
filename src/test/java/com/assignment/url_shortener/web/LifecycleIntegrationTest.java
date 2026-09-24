package com.assignment.url_shortener.web;

import com.assignment.url_shortener.domain.ShortLinkRepository;
import com.assignment.url_shortener.service.ShortLinkService;
import com.assignment.url_shortener.support.TestClockConfiguration;
import com.assignment.url_shortener.support.TestClockConfiguration.MutableClock;
import com.assignment.url_shortener.web.dto.CreateLinkRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfiguration.class)
class LifecycleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ShortLinkRepository repository;
    @Autowired ShortLinkService service;
    @Autowired MutableClock clock;

    @BeforeEach void reset() { repository.deleteAll(); clock.set(MutableClock.START); }

    private void create(String extra) throws Exception {
        mvc.perform(post("/api/v1/links").contentType("application/json")
                .content("{\"originalUrl\":\"https://example.com/docs\",\"customAlias\":\"demo-link\"" + extra + "}"))
                .andExpect(status().isCreated());
    }

    @Test void countsGetsButNotHeadOrMetadata() throws Exception {
        create("");
        mvc.perform(get("/api/v1/links/demo-link/analytics"))
                .andExpect(jsonPath("$.totalClicks").value(0)).andExpect(jsonPath("$.lastClickedAt").isEmpty());
        mvc.perform(head("/r/demo-link")).andExpect(status().isFound()).andExpect(content().string(""));
        mvc.perform(get("/api/v1/links/demo-link")).andExpect(status().isOk());
        mvc.perform(get("/r/demo-link")).andExpect(status().isFound());
        mvc.perform(get("/r/demo-link")).andExpect(status().isFound());
        mvc.perform(get("/api/v1/links/demo-link/analytics"))
                .andExpect(jsonPath("$.totalClicks").value(2))
                .andExpect(jsonPath("$.lastClickedAt").value("2026-01-01T12:00:00Z"));
    }

    @Test void expirationBoundaryIsExclusiveAndDoesNotCountFailures() throws Exception {
        create(",\"expiresAt\":\"2026-01-01T12:00:01Z\"");
        clock.set(MutableClock.START.plusMillis(999));
        mvc.perform(get("/r/demo-link")).andExpect(status().isFound());
        clock.set(MutableClock.START.plusSeconds(1));
        mvc.perform(get("/r/demo-link")).andExpect(status().isGone());
        mvc.perform(head("/r/demo-link")).andExpect(status().isGone());
        mvc.perform(get("/api/v1/links/demo-link"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED"));
        mvc.perform(get("/api/v1/links/demo-link/analytics")).andExpect(jsonPath("$.totalClicks").value(1));
    }

    @Test void disableIsIdempotentAndPreservesAnalyticsAndAlias() throws Exception {
        create("");
        mvc.perform(get("/r/demo-link")).andExpect(status().isFound());
        mvc.perform(delete("/api/v1/links/demo-link")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/links/demo-link")).andExpect(status().isNoContent());
        mvc.perform(get("/r/demo-link")).andExpect(status().isGone());
        mvc.perform(head("/r/demo-link")).andExpect(status().isGone());
        mvc.perform(get("/api/v1/links/demo-link")).andExpect(jsonPath("$.status").value("DISABLED"));
        mvc.perform(get("/api/v1/links/demo-link/analytics")).andExpect(jsonPath("$.totalClicks").value(1));
        mvc.perform(post("/api/v1/links").contentType("application/json")
                .content("{\"originalUrl\":\"https://other.example\",\"customAlias\":\"demo-link\"}"))
                .andExpect(status().isConflict());
    }

    @Test void customAliasIsCaseSensitive() throws Exception {
        service.create(new CreateLinkRequest("https://example.com", "Alias", null));
        service.create(new CreateLinkRequest("https://example.org", "alias", null));
        assertThat(service.preview("Alias")).isEqualTo("https://example.com");
        assertThat(service.preview("alias")).isEqualTo("https://example.org");
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad alias", "abc", "../bad", "", "abcdefghijklmnopqrstuvwxyz1234567"})
    void rejectsInvalidAliases(String alias) throws Exception {
        mvc.perform(post("/api/v1/links").contentType("application/json")
                .content("{\"originalUrl\":\"https://example.com\",\"customAlias\":\"" + alias + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.customAlias").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-01-01T12:00:00Z", "2020-01-01T00:00:00Z", "invalid"})
    void rejectsInvalidExpiration(String expiry) throws Exception {
        mvc.perform(post("/api/v1/links").contentType("application/json")
                .content("{\"originalUrl\":\"https://example.com\",\"expiresAt\":\"" + expiry + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void normalizesSubMicrosecondExpiration() {
        var link = service.create(new CreateLinkRequest("https://example.com", "precise", MutableClock.START.plusNanos(1999)));
        assertThat(link.expiresAt()).isEqualTo(MutableClock.START.plusNanos(1000));
        assertThat(service.get("precise").expiresAt()).isEqualTo(link.expiresAt());
    }

    @Test void unknownOperationsReturn404() throws Exception {
        mvc.perform(get("/api/v1/links/unknown")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/links/unknown/analytics")).andExpect(status().isNotFound());
        mvc.perform(head("/r/unknown")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/links/unknown")).andExpect(status().isNotFound());
    }

    @Test void malformedAndMissingBodyFieldsReturn400() throws Exception {
        for (String body : new String[]{"{}", "{", "{\"originalUrl\":null}", "{\"originalUrl\":\"\"}"}) {
            mvc.perform(post("/api/v1/links").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
    }

    @Test void concurrentRedirectsDoNotLoseClicks() throws Exception {
        create("");
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<String>>();
            for (int i = 0; i < 80; i++) tasks.add(() -> service.resolve("demo-link"));
            for (var future : pool.invokeAll(tasks, 20, TimeUnit.SECONDS)) {
                assertThat(future.get()).isEqualTo("https://example.com/docs");
            }
        }
        assertThat(service.analytics("demo-link").totalClicks()).isEqualTo(80);
    }

    @Test void concurrentAliasCreationHasExactlyOneWinner() throws Exception {
        try (var pool = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<Callable<Integer>>();
            for (int i = 0; i < 8; i++) tasks.add(() -> mvc.perform(post("/api/v1/links")
                    .contentType("application/json")
                    .content("{\"originalUrl\":\"https://example.com\",\"customAlias\":\"same-alias\"}"))
                    .andReturn().getResponse().getStatus());
            var results = new ArrayList<Integer>();
            for (var future : pool.invokeAll(tasks, 20, TimeUnit.SECONDS)) results.add(future.get());
            assertThat(results).filteredOn(code -> code == 201).hasSize(1);
            assertThat(results).filteredOn(code -> code == 409).hasSize(7);
            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Test void lastClickTimestampNeverMovesBackwards() throws Exception {
        create("");
        clock.set(MutableClock.START.plusSeconds(5));
        service.resolve("demo-link");
        clock.set(MutableClock.START.plusSeconds(2));
        service.resolve("demo-link");
        assertThat(service.analytics("demo-link").lastClickedAt()).isEqualTo(MutableClock.START.plusSeconds(5));
    }
}
