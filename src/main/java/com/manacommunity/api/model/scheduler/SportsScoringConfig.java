package com.manacommunity.api.model.scheduler;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "sports_scoring_config")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsScoringConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private SportsTournamentConfig config;

    @Column(nullable = false)
    private String sportType;

    @Builder.Default
    private Integer periodsCount = 2;

    private Integer pointsToWinPeriod;

    @Builder.Default
    private Boolean mustWinByTwo = false;

    private Integer periodsToWin;

    private Integer periodDurationMinutes;

    @Builder.Default
    private Boolean hasOvertime = false;

    @Builder.Default
    private Boolean hasPenaltyShootout = false;

    private Integer tiebreakPointsToWin;

    @Column(columnDefinition = "TEXT")
    private String scoringRulesJson;

    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
