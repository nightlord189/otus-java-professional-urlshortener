package org.aburavov.urlshortener.link;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<Link> findByOwnerIdAndStatus(Long ownerId, LinkStatus status, Pageable pageable);

    Page<Link> findByOwnerIdAndStatusNot(Long ownerId, LinkStatus status, Pageable pageable);
}
