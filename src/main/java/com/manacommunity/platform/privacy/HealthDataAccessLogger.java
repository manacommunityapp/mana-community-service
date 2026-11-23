package com.manacommunity.platform.privacy;

import com.manacommunity.api.platform.web.CorrelationContext;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persists every health data read in a separate transaction so a failure here
 * does NOT roll back the caller's read operation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthDataAccessLogger {

    private final HealthDataAccessLogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long accessorId, String accessorType, Long subjectUserId,
                    String recordType, String recordId, Long communityId,
                    String accessPurpose, String ipAddress) {
        try {
            HealthDataAccessLog entry = HealthDataAccessLog.builder()
                    .accessorId(accessorId)
                    .accessorType(accessorType)
                    .subjectUserId(subjectUserId)
                    .recordType(recordType)
                    .recordId(recordId)
                    .communityId(communityId)
                    .accessPurpose(accessPurpose)
                    .ipAddress(ipAddress)
                    .correlationId(CorrelationContext.get())
                    .build();
            logRepository.save(entry);
        } catch (Exception e) {
            // Access logging must never block health data reads
            log.warn("Health data access log failed: type={} id={} error={}",
                    recordType, recordId, e.getMessage());
        }
    }

    // ── Entity ─────────────────────────────────────────────────────────────

    @Entity
    @Table(name = "health_data_access_log")
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class HealthDataAccessLog {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "log_id", nullable = false, updatable = false)
        private UUID logId;

        @Column(name = "accessor_id", nullable = false)
        private Long accessorId;

        @Column(name = "accessor_type", nullable = false)
        private String accessorType;

        @Column(name = "subject_user_id", nullable = false)
        private Long subjectUserId;

        @Column(name = "record_type", nullable = false)
        private String recordType;

        @Column(name = "record_id", nullable = false)
        private String recordId;

        @Column(name = "community_id")
        private Long communityId;

        @Column(name = "access_purpose", length = 200)
        private String accessPurpose;

        @Column(name = "ip_address", length = 50)
        private String ipAddress;

        @Column(name = "correlation_id")
        private String correlationId;

        @CreationTimestamp
        @Column(name = "accessed_at", nullable = false, updatable = false)
        private OffsetDateTime accessedAt;

        @PrePersist
        void prePersist() {
            if (logId == null) logId = UUID.randomUUID();
        }
    }
}
