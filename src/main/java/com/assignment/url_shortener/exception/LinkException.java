package com.assignment.url_shortener.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LinkException extends RuntimeException {
    private final HttpStatus status;
    public LinkException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
