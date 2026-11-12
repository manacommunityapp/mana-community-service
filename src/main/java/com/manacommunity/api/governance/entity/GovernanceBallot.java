package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "governance_ballot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernanceBallot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, CLOSED, CANCELLED

    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson; // JSON array of {id, label}

    @Column(name = "votes_cast")
    @Builder.Default
    private int votesCast = 0;

    @Column(name = "total_eligible")
    private int totalEligible;

    @Column(name = "close_at")
    private LocalDateTime closeAt;

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
