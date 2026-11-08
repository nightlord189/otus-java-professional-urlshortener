package org.aburavov.urlshortener.repository;

import org.aburavov.urlshortener.model.Link;
import org.aburavov.urlshortener.model.LinkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<Link> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Link> findByOwnerIdAndStatus(Long ownerId, LinkStatus status, Pageable pageable);

    Page<Link> findByOwnerIdAndStatusNot(Long ownerId, LinkStatus status, Pageable pageable);

    Page<Link> findByStatus(LinkStatus status, Pageable pageable);

    long countByStatus(LinkStatus status);

    long countByStatusNot(LinkStatus status);
}
