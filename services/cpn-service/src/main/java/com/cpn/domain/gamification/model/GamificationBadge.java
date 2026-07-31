package com.cpn.domain.gamification.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "gamification_badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamificationBadge extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String badgeName; // VERIFIED_MENTOR, TOP_REFERRAL, THOUGHT_LEADER, HACKATHON_WINNER

    private String badgeCategory;
    private Integer reputationPoints;
    private boolean isUnlocked;
}
