package com.assignment.url_shortener.validation;

import com.assignment.url_shortener.exception.LinkException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.URISyntaxException;

@Component
public class UrlPolicy {
    public void validate(String value) {
        try {
            if (value == null || value.length() > 2048 || value.chars().anyMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c))) {
                throw new URISyntaxException("", "Invalid length or whitespace");
            }
            URI uri = new URI(value);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getPort() > 65535) {
                throw new URISyntaxException("", "Unsupported destination");
            }
        } catch (URISyntaxException exception) {
            throw new LinkException(HttpStatus.BAD_REQUEST,
                    "originalUrl must be an absolute HTTP(S) URL without credentials or whitespace (maximum 2048 characters)");
        }
    }
}
