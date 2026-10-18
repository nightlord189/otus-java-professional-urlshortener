package org.aburavov.urlshortener.link.dto;

import java.time.Instant;
import org.aburavov.urlshortener.link.LinkStatus;

public record UpdateLinkRequest(LinkStatus status, Instant expiresAt) {
}
