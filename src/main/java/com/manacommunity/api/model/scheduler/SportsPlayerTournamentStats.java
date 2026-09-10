package com.manacommunity.api.model.scheduler;

import com.manacommunity.api.model.SportsAuctionPlayer;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "sports_player_tournament_stats",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_config_id", "player_id"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsPlayerTournamentStats {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_config_id", nullable = false)
    private SportsTournamentConfig config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private SportsAuctionPlayer player;

    @Builder.Default private Integer matchesPlayed     = 0;
    @Builder.Default private Integer inningsBatted     = 0;
    @Builder.Default private Integer inningsBowled     = 0;
    @Builder.Default private Integer totalRuns          = 0;
    @Builder.Default private Integer highestScore       = 0;
    @Builder.Default private BigDecimal battingAverage  = BigDecimal.ZERO;
    @Builder.Default private BigDecimal battingStrikeRate = BigDecimal.ZERO;
    @Builder.Default private Integer fifties            = 0;
    @Builder.Default private Integer hundreds           = 0;
    @Builder.Default private Integer fours              = 0;
    @Builder.Default private Integer sixes              = 0;
    @Builder.Default private Integer totalWickets       = 0;
    private String bestBowling;
    @Builder.Default private BigDecimal bowlingAverage  = BigDecimal.ZERO;
    @Builder.Default private BigDecimal economyRate     = BigDecimal.ZERO;
    @Builder.Default private Integer catches            = 0;
    @Builder.Default private Integer stumpings          = 0;
    @Builder.Default private Integer runOuts            = 0;
    @Builder.Default private Integer manOfMatchCount    = 0;

    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate void onSave() { updatedAt = LocalDateTime.now(); }
}
