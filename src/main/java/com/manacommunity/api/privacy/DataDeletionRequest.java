package com.manacommunity.api.privacy;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_deletion_request", indexes = {
    @Index(name = "idx_ddr_user",   columnList = "user_id"),
    @Index(name = "idx_ddr_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDeletionRequest {

    public enum DeletionStatus {
        PENDING,
        VERIFIED,
        PROCESSING,
        COMPLETED,
        REJECTED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "community_id")
    private Long communityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private DeletionStatus status = DeletionStatus.PENDING;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    @Column(name = "verification_expiry")
    private LocalDateTime verificationExpiry;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
    }
}
