package com.manacommunity.api.wellness.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wellness_challenge", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessChallenge extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false)
    private ChallengeType challengeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChallengeStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "target_value")
    private Integer targetValue;

    @Column(name = "target_unit")
    private String targetUnit;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "badge_name")
    private String badgeName;

    @Column(name = "badge_icon")
    private String badgeIcon;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ChallengeParticipant> participants = new ArrayList<>();
}
