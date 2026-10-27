package com.manacommunity.api.mobility.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "mobility_carpool_booking", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mobility_carpool_booking_offer_user", columnNames = {"offer_id", "user_id"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MobilityCarpoolBooking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "offer_id", nullable = false)
    private MobilityCarpoolOffer offer;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false) private short seats = 1;
    @Column(nullable = false, length = 20) private String status = "CONFIRMED";
    @Column(name = "booked_at", nullable = false, updatable = false) private Instant bookedAt = Instant.now();
}
