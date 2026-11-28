package com.manacommunity.api.guardops.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "guard_gamification_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GamificationEvent extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long guardId;
    private String guardName;
    private String communityId;

    @Enumerated(EnumType.STRING)
    private AchievementType achievementType;

    private int pointsAwarded;
    private String description;
    private LocalDateTime earnedAt;

    @Enumerated(EnumType.STRING)
    private BadgeLevel badgeLevel;

    private String badgeName;
    private boolean notified;

    public enum AchievementType {
        ON_TIME_CLOCK_IN, ZERO_INCIDENT_SHIFT, FAST_VISITOR_PROCESSING,
        PERFECT_HANDOVER, PACKAGE_DELIVERY_STREAK, EMERGENCY_RESPONSE,
        MONTHLY_ATTENDANCE_100, COMMUNITY_APPRECIATION, TRAINING_COMPLETED,
        EQUIPMENT_MAINTENANCE
    }

    public enum BadgeLevel { NONE, BRONZE_BADGE, SILVER_BADGE, GOLD_BADGE, DIAMOND_BADGE }
}
