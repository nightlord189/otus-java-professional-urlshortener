package org.aburavov.urlshortener.dto;

import java.time.Instant;

public record CreateLinkRequest(String originalUrl, String alias, Instant expiresAt) {}
