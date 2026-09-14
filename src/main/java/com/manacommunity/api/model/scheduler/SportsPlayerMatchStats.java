package com.manacommunity.api.model.scheduler;

import com.manacommunity.api.model.SportsAuctionPlayer;
import com.manacommunity.api.model.SportsAuctionTeam;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "sports_player_match_stats")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsPlayerMatchStats {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private SportsTournamentMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private SportsAuctionPlayer player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private SportsAuctionTeam team;

    @Column(nullable = false)
    private String sportType;

    @Column(columnDefinition = "TEXT")
    private String statsJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist  void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   void onUpdate() { updatedAt = LocalDateTime.now(); }
}
