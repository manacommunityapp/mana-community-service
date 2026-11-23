package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "noc_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NocRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "noc_type", nullable = false, length = 50)
    private String nocType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supporting_docs", columnDefinition = "jsonb")
    private String supportingDocs;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "noc_number", length = 100, unique = true)
    private String nocNumber;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
