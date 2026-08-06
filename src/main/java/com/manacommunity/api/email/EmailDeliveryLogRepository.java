package com.manacommunity.api.email;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailDeliveryLogRepository extends JpaRepository<EmailDeliveryLog, Long> {

    /**
     * Filtered, paginated query used by the admin dashboard endpoint.
     * All parameters are optional — pass null to skip that filter.
     */
    @Query("""
            SELECT l FROM EmailDeliveryLog l
            WHERE (:status       IS NULL OR l.status       = :status)
              AND (:templateType IS NULL OR l.templateType = :templateType)
              AND (:communityId  IS NULL OR l.communityId  = :communityId)
              AND (:recipient    IS NULL OR LOWER(l.recipient) LIKE LOWER(CONCAT('%',:recipient,'%')))
              AND (:from         IS NULL OR l.sentAt >= :from)
              AND (:to           IS NULL OR l.sentAt <= :to)
            ORDER BY l.sentAt DESC
            """)
    Page<EmailDeliveryLog> findFiltered(
            @Param("status")       String status,
            @Param("templateType") String templateType,
            @Param("communityId")  Long communityId,
            @Param("recipient")    String recipient,
            @Param("from")         LocalDateTime from,
            @Param("to")           LocalDateTime to,
            Pageable pageable);

    /** Count failed emails for a community in a time window — used by admin summary. */
    long countByCommunityIdAndStatusAndSentAtAfter(Long communityId, String status, LocalDateTime after);

    /** Resolve a tracking token for open-tracking (Level 2). */
    Optional<EmailDeliveryLog> findByTrackingToken(String trackingToken);
}
