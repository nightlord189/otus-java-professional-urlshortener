package org.aburavov.urlshortener.stats;

import java.time.Instant;
import org.aburavov.urlshortener.stats.dto.LinkStatsResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links/{id}/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public LinkStatsResponse linkStats(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                       @RequestParam(required = false) Instant from,
                                       @RequestParam(required = false) Instant to) {
        return statsService.linkStats(Long.parseLong(jwt.getSubject()), id, from, to);
    }
}
