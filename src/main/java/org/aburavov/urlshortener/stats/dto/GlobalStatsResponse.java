package org.aburavov.urlshortener.stats.dto;

import java.util.List;

public record GlobalStatsResponse(
        long totalUsers,
        long totalLinks,
        long activeLinks,
        long totalClicks,
        long clicksLast24h,
        List<TopLinkResponse> topLinks) {
}
