package org.aburavov.urlshortener.controller;

import org.aburavov.urlshortener.dto.LinkStatsResponse;
import org.aburavov.urlshortener.model.User;
import org.aburavov.urlshortener.service.StatsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/api/v1/links/{id}/stats")
    public LinkStatsResponse linkStats(
            @AuthenticationPrincipal User user,
            @PathVariable long id,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return statsService.linkStats(user.getId(), id, from, to);
    }
}
