package org.aburavov.urlshortener.service;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class ShortCodeGenerator {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final Random random = new Random();

    public String generate() {
        var code = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
