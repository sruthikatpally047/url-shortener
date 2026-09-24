package com.assignment.url_shortener.service;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class CodeGenerator {
    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        var result = new StringBuilder(8);
        for (int i = 0; i < 8; i++) result.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        return result.toString();
    }
}
