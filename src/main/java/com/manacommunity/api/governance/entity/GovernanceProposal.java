package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "governance_proposal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernanceProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category; // INFRASTRUCTURE, FINANCE, SECURITY, GOVERNANCE, AMENITIES

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN"; // OPEN, UNDER_REVIEW, APPROVED, REJECTED, WITHDRAWN

    @Column(name = "support_count")
    @Builder.Default
    private int supportCount = 0;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    @Column(name = "meeting_id")
    private Long meetingId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
