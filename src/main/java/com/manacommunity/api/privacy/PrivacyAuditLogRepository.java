package com.manacommunity.api.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrivacyAuditLogRepository extends JpaRepository<PrivacyAuditLog, Long> {

    List<PrivacyAuditLog> findByActorIdOrderByTimestampDesc(Long actorId);

    List<PrivacyAuditLog> findByResourceTypeAndResourceIdOrderByTimestampDesc(
            String resourceType, String resourceId);

    List<PrivacyAuditLog> findByActionOrderByTimestampDesc(String action);

    List<PrivacyAuditLog> findByTimestampAfterOrderByTimestampDesc(LocalDateTime since);

    /** Count how many times a given actor has triggered privacy events since a timestamp. */
    long countByActorIdAndTimestampAfter(Long actorId, LocalDateTime since);
}
