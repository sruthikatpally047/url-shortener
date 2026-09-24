package com.assignment.url_shortener.validation;

import com.assignment.url_shortener.exception.LinkException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class UrlPolicyTest {
    private final UrlPolicy policy = new UrlPolicy();

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "http://example.com:8080/path?q=a%20b#section", "HTTPS://example.com", "http://[::1]:8080/a"})
    void acceptsAbsoluteHttpUrls(String url) { assertThatCode(() -> policy.validate(url)).doesNotThrowAnyException(); }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "file:///etc/passwd", "ftp://example.com", "/relative", "//example.com",
            "https://user:password@example.com", "https://example.com/path with spaces", "https://", "https://example.com:99999", "https://example.com/\r\nheader:value"})
    void rejectsUnsafeOrMalformedUrls(String url) { assertThatThrownBy(() -> policy.validate(url)).isInstanceOf(LinkException.class); }

    @Test void rejectsNullAndOversizedUrls() {
        assertThatThrownBy(() -> policy.validate(null)).isInstanceOf(LinkException.class);
        assertThatThrownBy(() -> policy.validate("https://example.com/" + "a".repeat(2048))).isInstanceOf(LinkException.class);
    }
}
