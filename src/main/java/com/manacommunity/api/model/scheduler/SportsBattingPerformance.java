package com.manacommunity.api.model.scheduler;

import com.manacommunity.api.model.SportsAuctionPlayer;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "sports_batting_performance")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsBattingPerformance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "innings_id", nullable = false)
    private SportsMatchInnings innings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private SportsAuctionPlayer player;

    @Builder.Default private Integer battingPosition = 0;
    @Builder.Default private Integer runsScored      = 0;
    @Builder.Default private Integer ballsFaced      = 0;
    @Builder.Default private Integer fours            = 0;
    @Builder.Default private Integer sixes            = 0;
    @Builder.Default private BigDecimal strikeRate   = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private DismissalType dismissalType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dismissed_by_id")
    private SportsAuctionPlayer dismissedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fielder_id")
    private SportsAuctionPlayer fielder;

    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
