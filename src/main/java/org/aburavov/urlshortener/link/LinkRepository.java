package org.aburavov.urlshortener.link;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<Link> findByOwnerIdAndStatus(Long ownerId, LinkStatus status, Pageable pageable);

    Page<Link> findByOwnerIdAndStatusNot(Long ownerId, LinkStatus status, Pageable pageable);

    @Query("""
            select l from Link l
            where (:ownerId is null or l.owner.id = :ownerId)
              and (:status is null or l.status = :status)
            """)
    Page<Link> findAllFiltered(@Param("ownerId") Long ownerId, @Param("status") LinkStatus status, Pageable pageable);

    long countByStatus(LinkStatus status);

    long countByStatusNot(LinkStatus status);
}
