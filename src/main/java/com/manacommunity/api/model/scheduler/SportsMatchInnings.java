package com.manacommunity.api.model.scheduler;

import com.manacommunity.api.model.SportsAuctionTeam;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "sports_match_innings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsMatchInnings {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_result_id", nullable = false)
    private SportsMatchResult matchResult;

    @Column(nullable = false)
    @Builder.Default
    private Integer inningsNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batting_team_id", nullable = false)
    private SportsAuctionTeam battingTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bowling_team_id", nullable = false)
    private SportsAuctionTeam bowlingTeam;

    @Builder.Default private Integer totalRuns    = 0;
    @Builder.Default private Integer totalWickets = 0;
    @Builder.Default private BigDecimal totalOvers = BigDecimal.ZERO;
    @Builder.Default private Integer extras       = 0;

    @Column(columnDefinition = "JSON")
    private String extrasDetail;

    private Integer target;

    @Builder.Default
    private BigDecimal runRate = BigDecimal.ZERO;

    @Column(columnDefinition = "JSON")
    private String fallOfWickets;

    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
