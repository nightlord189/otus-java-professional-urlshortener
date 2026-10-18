package org.aburavov.urlshortener.stats;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.aburavov.urlshortener.common.BadRequestException;
import org.aburavov.urlshortener.link.Link;
import org.aburavov.urlshortener.link.LinkService;
import org.aburavov.urlshortener.stats.dto.DailyClicksResponse;
import org.aburavov.urlshortener.stats.dto.GlobalStatsResponse;
import org.aburavov.urlshortener.stats.dto.LinkStatsResponse;
import org.aburavov.urlshortener.stats.dto.RefererClicksResponse;
import org.aburavov.urlshortener.stats.dto.TopLinkResponse;
import org.aburavov.urlshortener.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    private static final int DEFAULT_PERIOD_DAYS = 30;
    private static final int MAX_PERIOD_DAYS = 366;
    private static final int TOP_LIMIT = 10;

    private final ClickRepository clickRepository;
    private final LinkService linkService;
    private final UserService userService;

    public StatsService(ClickRepository clickRepository, LinkService linkService, UserService userService) {
        this.clickRepository = clickRepository;
        this.linkService = linkService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public LinkStatsResponse linkStats(long ownerId, long linkId, Instant from, Instant to) {
        Link link = linkService.getOwned(ownerId, linkId);
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(DEFAULT_PERIOD_DAYS, ChronoUnit.DAYS) : from;
        if (!start.isBefore(end)) {
            throw new BadRequestException("'from' must be before 'to'");
        }
        if (Duration.between(start, end).toDays() > MAX_PERIOD_DAYS) {
            throw new BadRequestException("Period must not exceed one year");
        }

        long totalClicks = clickRepository.countInPeriod(link.getId(), start, end);
        List<DailyClicksResponse> clicksByDay = clickRepository.countByDay(link.getId(), start, end).stream()
                .map(day -> new DailyClicksResponse(day.getDay(), day.getClicks()))
                .toList();
        List<RefererClicksResponse> topReferers = clickRepository.topReferers(link.getId(), start, end, TOP_LIMIT).stream()
                .map(referer -> new RefererClicksResponse(referer.getReferer(), referer.getClicks()))
                .toList();
        return new LinkStatsResponse(link.getId(), start, end, totalClicks, clicksByDay, topReferers);
    }

    @Transactional(readOnly = true)
    public GlobalStatsResponse globalStats() {
        Instant dayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        List<TopLinkResponse> topLinks = clickRepository.topLinks(TOP_LIMIT).stream()
                .map(top -> new TopLinkResponse(top.getLinkId(), top.getCode(), top.getClicks()))
                .toList();
        return new GlobalStatsResponse(
                userService.count(),
                linkService.countNotDeleted(),
                linkService.countActive(),
                clickRepository.count(),
                clickRepository.countByClickedAtAfter(dayAgo),
                topLinks);
    }
}
