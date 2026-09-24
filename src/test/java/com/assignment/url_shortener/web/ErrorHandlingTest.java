package com.assignment.url_shortener.web;

import com.assignment.url_shortener.service.ShortLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShortLinkController.class)
@Import(GlobalExceptionHandler.class)
class ErrorHandlingTest {
    @Autowired MockMvc mvc;
    @MockitoBean ShortLinkService service;

    @Test void connectionFailureAtTransactionStartIs503() throws Exception {
        when(service.get("test-link")).thenThrow(new CannotCreateTransactionException("private connection string"));
        mvc.perform(get("/api/v1/links/test-link"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(not(containsString("private connection"))));
    }

    @Test void storageFailureIs503WithoutDatabaseDetails() throws Exception {
        when(service.get("test-link")).thenThrow(new DataAccessResourceFailureException("secret database details"));
        mvc.perform(get("/api/v1/links/test-link"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.detail").value("Storage is temporarily unavailable; please retry"))
                .andExpect(content().string(not(containsString("secret"))));
    }
}
