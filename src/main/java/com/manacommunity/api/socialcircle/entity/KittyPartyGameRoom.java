package com.manacommunity.api.socialcircle.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "kitty_game_room", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KittyPartyGameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false)
    private KittyPartyCircle circle;

    @Column(name = "room_name", nullable = false, length = 150)
    private String roomName;

    @Column(name = "game_type", nullable = false, length = 30)
    @Builder.Default
    private String gameType = "TAMBOLA_90";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "WAITING"; // WAITING, IN_PROGRESS, PAUSED, COMPLETED, CANCELLED

    @Column(name = "ticket_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ticketPrice = BigDecimal.ZERO; // Free to play

    @Column(name = "max_players")
    @Builder.Default
    private Integer maxPlayers = 50;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
