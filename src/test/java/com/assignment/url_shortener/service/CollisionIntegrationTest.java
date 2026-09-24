package com.assignment.url_shortener.service;

import com.assignment.url_shortener.domain.ShortLinkRepository;
import com.assignment.url_shortener.exception.LinkException;
import com.assignment.url_shortener.web.dto.CreateLinkRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class CollisionIntegrationTest {
    @Autowired ShortLinkService service;
    @Autowired ShortLinkRepository repository;
    @MockitoBean CodeGenerator generator;
    @BeforeEach void resetData() { repository.deleteAll(); }

    @Test void retriesCollisionInFreshTransaction() {
        service.create(new CreateLinkRequest("https://example.com/first", "occupied", null));
        when(generator.generate()).thenReturn("occupied", "new-code");
        var result = service.create(new CreateLinkRequest("https://example.com/second", null, null));
        assertThat(result.code()).isEqualTo("new-code");
        assertThat(repository.count()).isEqualTo(2);
        verify(generator, times(2)).generate();
    }

    @Test void stopsAfterFiveCollisions() {
        service.create(new CreateLinkRequest("https://example.com", "occupied", null));
        when(generator.generate()).thenReturn("occupied");
        assertThatThrownBy(() -> service.create(new CreateLinkRequest("https://example.com", null, null)))
                .isInstanceOfSatisfying(LinkException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(generator, times(5)).generate();
        assertThat(repository.count()).isEqualTo(1);
    }
}
