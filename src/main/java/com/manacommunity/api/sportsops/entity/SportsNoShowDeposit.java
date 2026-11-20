package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sports_no_show_deposit")
@Data
public class SportsNoShowDeposit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "booking_id", nullable = false) private Long bookingId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "held_at") private Instant heldAt = Instant.now();
    @Column private Boolean released = false;
    @Column(name = "released_at") private Instant releasedAt;
    @Column private Boolean forfeited = false;
    @Column(name = "forfeited_at") private Instant forfeitedAt;
    @Column(length = 500) private String reason;
}
