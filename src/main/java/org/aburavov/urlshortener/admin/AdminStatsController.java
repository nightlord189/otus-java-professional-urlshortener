package org.aburavov.urlshortener.admin;

import org.aburavov.urlshortener.stats.StatsService;
import org.aburavov.urlshortener.stats.dto.GlobalStatsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public GlobalStatsResponse globalStats() {
        return statsService.globalStats();
    }
}
