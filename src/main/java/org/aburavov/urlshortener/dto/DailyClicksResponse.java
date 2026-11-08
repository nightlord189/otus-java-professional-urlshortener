package org.aburavov.urlshortener.dto;

import java.time.LocalDate;

public record DailyClicksResponse(LocalDate date, long clicks) {}
