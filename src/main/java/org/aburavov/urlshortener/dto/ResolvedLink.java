package org.aburavov.urlshortener.dto;

import org.aburavov.urlshortener.model.LinkStatus;

import java.io.Serializable;
import java.time.Instant;

public record ResolvedLink(long id, String originalUrl, LinkStatus status, Instant expiresAt) implements Serializable {

    public boolean isActive() {
        return status == LinkStatus.ACTIVE && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
}
