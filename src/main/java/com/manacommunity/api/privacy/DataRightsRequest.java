package com.manacommunity.api.privacy;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks DPDP Act 2023 s.12–13 data-principal rights requests.
 * Covers: ACCESS, CORRECTION, ERASURE, PORTABILITY, GRIEVANCE, NOMINATION.
 */
@Entity
@Table(name = "data_rights_requests", indexes = {
    @Index(name = "idx_drr_user", columnList = "user_id"),
    @Index(name = "idx_drr_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataRightsRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    /**
     * Right type: ACCESS | CORRECTION | ERASURE | PORTABILITY | GRIEVANCE | NOMINATION
     */
    @Column(name = "right_type", nullable = false, length = 30)
    private String rightType;

    @Column(name = "description", length = 2000)
    private String description;

    /** For CORRECTION requests: which field and new value. */
    @Column(name = "correction_details", length = 1000)
    private String correctionDetails;

    /** PENDING → ACKNOWLEDGED → IN_PROGRESS → COMPLETED | REJECTED */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** DPDP Act requires 30-day resolution; flag if approaching deadline. */
    @Column(name = "due_by")
    private LocalDateTime dueBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (dueBy == null) dueBy = LocalDateTime.now().plusDays(30);
        if (status == null) status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
