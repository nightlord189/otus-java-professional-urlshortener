package org.aburavov.urlshortener.stats.dto;

import java.time.LocalDate;

public record DailyClicksResponse(LocalDate date, long clicks) {
}
