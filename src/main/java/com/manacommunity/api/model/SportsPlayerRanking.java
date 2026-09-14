package com.manacommunity.api.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sports_player_ranking",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_sports_player_ranking",
        columnNames = {"user_id", "sport_id", "community_id", "season"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsPlayerRanking extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false)
    private SportsMeta sport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    /** Ordinal position: 1 = best ranked. Null = unranked. */
    @Column
    private Integer rank;

    /** Numeric score for balanced pairing (higher = better). Null = no rating. */
    @Column
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private RankingSource source = RankingSource.MANUAL;

    /** Season identifier e.g. "2025-26". Defaults to "CURRENT" for latest ranking. */
    @Column(length = 20, nullable = false)
    @Builder.Default
    private String season = "CURRENT";

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum RankingSource { MANUAL, COMPUTED }
}
