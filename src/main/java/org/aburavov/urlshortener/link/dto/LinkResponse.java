package org.aburavov.urlshortener.link.dto;

import java.time.Instant;
import org.aburavov.urlshortener.link.LinkStatus;

public record LinkResponse(
        Long id,
        String code,
        String shortUrl,
        String originalUrl,
        LinkStatus status,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt) {
}
