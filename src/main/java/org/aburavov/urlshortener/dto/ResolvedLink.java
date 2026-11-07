package org.aburavov.urlshortener.dto;

import org.aburavov.urlshortener.model.LinkStatus;

import java.time.Instant;

// what we keep in the cache: status and expiry are checked on every redirect
public record ResolvedLink(long id, String originalUrl, LinkStatus status, Instant expiresAt) {

    public boolean isActive() {
        return status == LinkStatus.ACTIVE && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
}
