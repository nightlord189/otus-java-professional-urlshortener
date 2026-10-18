package org.aburavov.urlshortener.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
