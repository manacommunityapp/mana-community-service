package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "governance_resolution")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernanceResolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resolution_number", nullable = false, length = 40)
    private String resolutionNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "passed_on")
    private LocalDate passedOn;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PASSED"; // PASSED, PENDING, CHALLENGED

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "meeting_id")
    private Long meetingId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
