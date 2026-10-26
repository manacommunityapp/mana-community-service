package com.manacommunity.api.mobility.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "mobility_driver_offer", indexes = {
    @Index(name = "idx_mob_offer_driver", columnList = "driver_id, offered_at DESC")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MobilityDriverOffer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private MobilityTrip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private MobilityDriver driver;

    @Column(name = "offered_at", nullable = false, updatable = false)
    private Instant offeredAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** null = pending, true = accepted, false = declined */
    private Boolean accepted;
}
