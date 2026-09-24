package com.assignment.url_shortener.web;

import com.assignment.url_shortener.domain.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShortLinkIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ShortLinkRepository repository;
    @BeforeEach void clean() { repository.deleteAll(); }

    @Test void createThenReadAndRedirect() throws Exception {
        mvc.perform(post("/api/v1/links").contentType("application/json")
                .content("{\"originalUrl\":\"https://example.com/path?q=one#section\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", startsWith("/api/v1/links/")))
                .andExpect(jsonPath("$.shortUrl", startsWith("https://sho.rt/r/")));
        String code = repository.findAll().getFirst().getCode();
        mvc.perform(get("/api/v1/links/{code}", code)).andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/path?q=one#section"));
        mvc.perform(get("/r/{code}", code)).andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/path?q=one#section"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void rejectsUnsafeScheme() throws Exception {
        mvc.perform(post("/api/v1/links").contentType("application/json")
                .content("{\"originalUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test void missingLinkIs404() throws Exception {
        mvc.perform(get("/r/missing")).andExpect(status().isNotFound());
    }

    @Test void swaggerAndHealthAreAvailable() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("URL Shortener API"))
                .andExpect(jsonPath("$.paths['/api/v1/links'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/r/{code}'].get.responses['302']").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
