package com.assignment.url_shortener.web;

import com.assignment.url_shortener.exception.LinkException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.TreeMap;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LinkException.class)
    ResponseEntity<ProblemDetail> domain(LinkException exception, HttpServletRequest request) {
        return problem(exception.getStatus(), exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var response = problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        var errors = new TreeMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        response.getBody().setProperty("fieldErrors", errors);
        return response;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> malformed(HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed JSON or invalid field type", request);
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    ResponseEntity<ProblemDetail> database(RuntimeException exception, HttpServletRequest request) {
        // Do not log SQL parameters or submitted URLs; they may contain sensitive query strings.
        log.error("Database operation failed: {}", exception.getClass().getSimpleName());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Storage is temporarily unavailable; please retry", request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, HttpServletRequest request) {
        var body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }
}
