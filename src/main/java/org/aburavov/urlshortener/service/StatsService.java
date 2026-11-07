package org.aburavov.urlshortener.service;

import org.aburavov.urlshortener.dto.*;
import org.aburavov.urlshortener.model.LinkStatus;
import org.aburavov.urlshortener.repository.ClickRepository;
import org.aburavov.urlshortener.repository.LinkRepository;
import org.aburavov.urlshortener.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class StatsService {

    private final ClickRepository clickRepository;
    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final LinkService linkService;
    private final JdbcTemplate jdbcTemplate;

    public StatsService(
            ClickRepository clickRepository,
            LinkRepository linkRepository,
            UserRepository userRepository,
            LinkService linkService,
            JdbcTemplate jdbcTemplate) {
        this.clickRepository = clickRepository;
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.linkService = linkService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public LinkStatsResponse linkStats(long ownerId, long linkId, Instant from, Instant to) {
        var link = linkService.getOwned(ownerId, linkId);
        if (to == null) {
            to = Instant.now();
        }
        if (from == null) {
            from = to.minus(30, ChronoUnit.DAYS);
        }
        if (!from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
        }

        var total = clickRepository.countByLinkIdAndClickedAtBetween(link.getId(), from, to);
        var byDay = jdbcTemplate.query(
                "select date(clicked_at) as day, count(*) as clicks from clicks"
                        + " where link_id = ? and clicked_at between ? and ? group by day order by day",
                (rs, i) -> new DailyClicksResponse(rs.getDate("day").toLocalDate(), rs.getLong("clicks")),
                link.getId(), Timestamp.from(from), Timestamp.from(to));
        var topReferers = jdbcTemplate.query(
                "select referer, count(*) as clicks from clicks"
                        + " where link_id = ? and clicked_at between ? and ? and referer is not null"
                        + " group by referer order by clicks desc limit 10",
                (rs, i) -> new RefererClicksResponse(rs.getString("referer"), rs.getLong("clicks")),
                link.getId(), Timestamp.from(from), Timestamp.from(to));
        return new LinkStatsResponse(link.getId(), from, to, total, byDay, topReferers);
    }

    public GlobalStatsResponse globalStats() {
        var topLinks = jdbcTemplate.query(
                "select c.link_id, l.short_code, count(*) as clicks from clicks c"
                        + " join links l on l.id = c.link_id"
                        + " group by c.link_id, l.short_code order by clicks desc limit 10",
                (rs, i) -> new TopLinkResponse(rs.getLong("link_id"), rs.getString("short_code"), rs.getLong("clicks")));
        return new GlobalStatsResponse(
                userRepository.count(),
                linkRepository.countByStatusNot(LinkStatus.DELETED),
                linkRepository.countByStatus(LinkStatus.ACTIVE),
                clickRepository.count(),
                clickRepository.countByClickedAtAfter(Instant.now().minus(1, ChronoUnit.DAYS)),
                topLinks);
    }
}
