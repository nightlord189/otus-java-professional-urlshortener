package org.aburavov.urlshortener.link.dto;

import java.time.Instant;

public record CreateLinkRequest(String originalUrl, String alias, Instant expiresAt) {
}
