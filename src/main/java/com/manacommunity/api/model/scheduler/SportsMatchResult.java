package com.manacommunity.api.model.scheduler;

import com.manacommunity.api.model.SportsAuctionPlayer;
import com.manacommunity.api.model.SportsAuctionTeam;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "sports_match_result")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsMatchResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private SportsTournamentMatch match;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false)
    @Builder.Default
    private ResultType resultType = ResultType.WIN;

    private String winMargin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toss_winner_team_id")
    private SportsAuctionTeam tossWinner;

    private String tossDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "man_of_match_player_id")
    private SportsAuctionPlayer manOfMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "best_batter_player_id")
    private SportsAuctionPlayer bestBatter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "best_bowler_player_id")
    private SportsAuctionPlayer bestBowler;

    @Column(columnDefinition = "TEXT")
    private String matchSummary;

    private String umpires;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
