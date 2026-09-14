package com.manacommunity.api.model.scheduler;

import com.manacommunity.api.model.SportsAuctionPlayer;
import com.manacommunity.api.model.SportsAuctionTeam;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "sports_race_result")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsRaceResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private SportsTournamentMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private SportsAuctionPlayer player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private SportsAuctionTeam team;

    private Integer heatNumber;
    private Integer laneNumber;

    private Long finishTimeMillis;

    private String formattedTime;

    @Enumerated(EnumType.STRING)
    private RaceStatus raceStatus;

    private Integer overallRank;
    private Integer heatRank;

    private Long personalBestMillis;

    @Builder.Default
    private Boolean isPersonalBest = false;

    @Column(columnDefinition = "TEXT")
    private String splitTimes;

    private String notes;

    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
