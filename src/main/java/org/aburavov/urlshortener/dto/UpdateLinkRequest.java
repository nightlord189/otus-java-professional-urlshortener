package org.aburavov.urlshortener.dto;

import org.aburavov.urlshortener.model.LinkStatus;

import java.time.Instant;

public record UpdateLinkRequest(LinkStatus status, Instant expiresAt) {}
