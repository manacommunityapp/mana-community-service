package com.manacommunity.api.mobility.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "mobility_journey_slot", indexes = {
        @Index(name = "idx_mobility_journey_slot_lookup", columnList = "community_id, journey_type, hub_code, status")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MobilityJourneySlot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "community_id", nullable = false) private Community community;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", nullable = false) private AppUser createdBy;
    @Column(name = "journey_type", nullable = false, length = 30) private String journeyType;
    @Column(nullable = false, length = 20) private String direction;
    @Column(name = "hub_code", nullable = false, length = 30) private String hubCode;
    @Column(name = "origin", nullable = false, columnDefinition = "TEXT") private String origin;
    @Column(name = "destination", nullable = false, columnDefinition = "TEXT") private String destination;
    @Column(name = "departure_time", nullable = false) private LocalTime departureTime;
    @Column(name = "days_of_week", nullable = false, length = 40) private String daysOfWeek;
    @Column(name = "seat_capacity", nullable = false) private short seatCapacity = 4;
    @Column(name = "luggage_capacity", nullable = false) private short luggageCapacity = 0;
    @Column(name = "price_per_seat", nullable = false, precision = 7, scale = 2) private BigDecimal pricePerSeat = BigDecimal.ZERO;
    @Column(nullable = false, length = 20) private String status = "ACTIVE";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
