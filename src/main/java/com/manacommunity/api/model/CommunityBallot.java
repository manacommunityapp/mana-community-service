package com.manacommunity.api.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_ballot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityBallot extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false)
    private String title;

    @Column
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "resolution_motion_text", columnDefinition = "TEXT")
    private String resolutionMotionText;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "total_eligible_voters")
    @Builder.Default
    private Integer totalEligibleVoters = 100;

    @Column(name = "min_quorum_percentage")
    @Builder.Default
    private Integer minQuorumPercentage = 50;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "UPCOMING"; // UPCOMING, ACTIVE, CLOSED

    @OneToMany(mappedBy = "ballot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<BallotOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "ballot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<BallotVote> votes = new ArrayList<>();
}
