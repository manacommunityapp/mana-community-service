package com.manacommunity.api.model.scheduler;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "sports_match_period_score")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsMatchPeriodScore {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private SportsTournamentMatch match;

    @Column(nullable = false)
    private Integer periodNumber;

    private String periodLabel;

    @Builder.Default
    private Integer scoreTeamA = 0;

    @Builder.Default
    private Integer scoreTeamB = 0;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
