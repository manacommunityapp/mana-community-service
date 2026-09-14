package com.manacommunity.api.model.scheduler;

import com.manacommunity.api.model.SportsAuctionPlayer;
import com.manacommunity.api.model.SportsAuctionTeam;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "sports_match_event")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsMatchEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private SportsTournamentMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private SportsAuctionTeam team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private SportsAuctionPlayer player;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Integer periodNumber;

    private Integer matchMinute;

    @Builder.Default
    private Integer pointsAwarded = 0;

    private String description;

    private Long secondaryPlayerId;

    @Builder.Default
    private Boolean isUndone = false;

    private Long createdBy;
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
