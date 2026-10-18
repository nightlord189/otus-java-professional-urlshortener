package org.aburavov.urlshortener.stats;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickRepository extends JpaRepository<Click, Long> {

    @Query("select count(c) from Click c where c.linkId = :linkId and c.clickedAt >= :from and c.clickedAt < :to")
    long countInPeriod(@Param("linkId") Long linkId, @Param("from") Instant from, @Param("to") Instant to);

    long countByClickedAtAfter(Instant since);

    @Query(value = """
            SELECT (clicked_at AT TIME ZONE 'UTC')::date AS day, count(*) AS clicks
            FROM clicks
            WHERE link_id = :linkId AND clicked_at >= :from AND clicked_at < :to
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<DailyClicks> countByDay(@Param("linkId") Long linkId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT referer, count(*) AS clicks
            FROM clicks
            WHERE link_id = :linkId AND clicked_at >= :from AND clicked_at < :to AND referer IS NOT NULL
            GROUP BY referer
            ORDER BY clicks DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<RefererClicks> topReferers(@Param("linkId") Long linkId, @Param("from") Instant from,
                                    @Param("to") Instant to, @Param("limit") int limit);

    @Query(value = """
            SELECT c.link_id AS "linkId", l.short_code AS code, count(*) AS clicks
            FROM clicks c
            JOIN links l ON l.id = c.link_id
            GROUP BY c.link_id, l.short_code
            ORDER BY clicks DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopLink> topLinks(@Param("limit") int limit);
}
