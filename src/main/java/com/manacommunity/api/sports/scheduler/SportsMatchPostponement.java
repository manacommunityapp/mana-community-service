package com.manacommunity.api.sports.scheduler;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_match_postponement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsMatchPostponement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private SportsTournamentMatch match;

    @Column(nullable = false)
    private LocalDateTime originalStartAt;

    private LocalDateTime proposedStartAt;
    private LocalDateTime newStartAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostponementReason reasonCode;

    @Column(columnDefinition = "TEXT")
    private String reasonDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PostponementScope scope = PostponementScope.SINGLE_MATCH;

    private String requestedBy;
    private String approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    private Long reserveDayId;

    @Column(columnDefinition = "TEXT")
    private String affectedMatchIdsJson;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public enum PostponementReason {
        RAIN, VENUE_UNAVAILABLE, COURT_MAINTENANCE, FLOODING,
        REFEREE_UNAVAILABLE, PLAYER_INJURY, TEAM_REQUEST,
        LOCAL_RESTRICTIONS, POWER_FAILURE, ORGANIZER_DECISION,
        PREVIOUS_MATCH_OVERTIME, PUBLIC_HOLIDAY, TRANSPORT_DISRUPTION,
        SAFETY_CONCERNS, INSUFFICIENT_LIGHT, OTHER
    }

    public enum PostponementScope {
        SINGLE_MATCH, MULTIPLE_MATCHES, ENTIRE_ROUND, ENTIRE_STAGE
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
}
