package com.manacommunity.api.socialcircle.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kitty_game_winner", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KittyPartyGameWinner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private KittyPartyGameRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private KittyPartyGameTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "claim_type", nullable = false, length = 40)
    private String claimType; // EARLY_FIVE, FOUR_CORNERS, TOP_LINE, MIDDLE_LINE, BOTTOM_LINE, FULL_HOUSE

    @Column(name = "claim_status", nullable = false, length = 30)
    @Builder.Default
    private String claimStatus = "VALIDATED"; // PENDING, VALIDATED, BOGUS

    @Column(name = "claimed_at", nullable = false, updatable = false)
    private LocalDateTime claimedAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @PrePersist
    protected void onCreate() {
        claimedAt = LocalDateTime.now();
        if (validatedAt == null) {
            validatedAt = claimedAt;
        }
    }
}
