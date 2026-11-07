package org.aburavov.urlshortener.dto;

public record TopLinkResponse(long linkId, String code, long clicks) {}
