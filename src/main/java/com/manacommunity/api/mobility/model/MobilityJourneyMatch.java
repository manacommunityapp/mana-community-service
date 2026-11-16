package com.manacommunity.api.mobility.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "mobility_journey_match", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mobility_journey_match_request_offer", columnNames = {"request_id", "offer_id"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MobilityJourneyMatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "request_id", nullable = false) private MobilityAirportRideRequest request;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "offer_id", nullable = false) private MobilityCarpoolOffer offer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "requester_id", nullable = false) private AppUser requester;
    @Column(nullable = false, length = 20) private String status = "PROPOSED";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "responded_at") private Instant respondedAt;
}
