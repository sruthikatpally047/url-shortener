package com.assignment.url_shortener.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

@TestConfiguration
public class TestClockConfiguration {
    @Bean @Primary
    public MutableClock testClock() { return new MutableClock(); }

    public static class MutableClock extends Clock {
        public static final Instant START = Instant.parse("2026-01-01T12:00:00Z");
        private final AtomicReference<Instant> value = new AtomicReference<>(START);
        public void set(Instant instant) { value.set(instant); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return value.get(); }
    }
}
