package org.aburavov.urlshortener.repository;

import org.aburavov.urlshortener.model.Click;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface ClickRepository extends JpaRepository<Click, Long> {

    long countByLinkIdAndClickedAtBetween(Long linkId, Instant from, Instant to);

    long countByClickedAtAfter(Instant since);
}
