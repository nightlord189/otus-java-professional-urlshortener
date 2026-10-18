package org.aburavov.urlshortener.stats.dto;

import java.time.Instant;
import java.util.List;

public record LinkStatsResponse(
        long linkId,
        Instant from,
        Instant to,
        long totalClicks,
        List<DailyClicksResponse> clicksByDay,
        List<RefererClicksResponse> topReferers) {
}
