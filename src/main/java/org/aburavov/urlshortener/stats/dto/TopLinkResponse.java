package org.aburavov.urlshortener.stats.dto;

public record TopLinkResponse(Long linkId, String code, long clicks) {
}
