package org.aburavov.urlshortener.redirect;

import java.time.Instant;
import org.aburavov.urlshortener.link.LinkStatus;

public record ResolvedLink(long id, String originalUrl, LinkStatus status, Instant expiresAt) {

    public boolean isActive() {
        return status == LinkStatus.ACTIVE && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
}
