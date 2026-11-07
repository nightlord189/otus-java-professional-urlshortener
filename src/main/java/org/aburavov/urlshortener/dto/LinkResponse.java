package org.aburavov.urlshortener.dto;

import org.aburavov.urlshortener.model.Link;
import org.aburavov.urlshortener.model.LinkStatus;

import java.time.Instant;

public record LinkResponse(Long id, Long ownerId, String code, String shortUrl, String originalUrl, LinkStatus status,
                           Instant expiresAt, Instant createdAt, Instant updatedAt) {

    public static LinkResponse from(Link link, String baseUrl) {
        return new LinkResponse(link.getId(), link.getOwner().getId(), link.getShortCode(),
                baseUrl + "/" + link.getShortCode(), link.getOriginalUrl(), link.getStatus(),
                link.getExpiresAt(), link.getCreatedAt(), link.getUpdatedAt());
    }
}
