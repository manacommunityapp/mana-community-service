package com.manacommunity.api.wellness.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "challenge_participant", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeParticipant extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private WellnessChallenge challenge;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "current_progress")
    private Integer currentProgress;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @Column(name = "completed")
    private Boolean completed;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "badge_earned")
    private Boolean badgeEarned;
}
