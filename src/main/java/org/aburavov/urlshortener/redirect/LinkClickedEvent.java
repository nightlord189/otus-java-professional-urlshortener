package org.aburavov.urlshortener.redirect;

import java.time.Instant;

public record LinkClickedEvent(long linkId, Instant clickedAt, String referer, String userAgent, String clientIp) {
}
