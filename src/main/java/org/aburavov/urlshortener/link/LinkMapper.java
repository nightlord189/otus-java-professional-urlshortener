package org.aburavov.urlshortener.link;

import org.aburavov.urlshortener.link.dto.LinkResponse;
import org.springframework.stereotype.Component;

@Component
public class LinkMapper {

    private final String baseUrl;

    public LinkMapper(LinkProperties properties) {
        this.baseUrl = properties.baseUrl();
    }

    public LinkResponse toResponse(Link link) {
        return new LinkResponse(
                link.getId(),
                link.getOwner().getId(),
                link.getShortCode(),
                baseUrl + "/" + link.getShortCode(),
                link.getOriginalUrl(),
                link.getStatus(),
                link.getExpiresAt(),
                link.getCreatedAt(),
                link.getUpdatedAt());
    }
}
