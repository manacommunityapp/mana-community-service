package com.manacommunity.api.mobility.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    @Builder.Default @Column(name = "seat_capacity", nullable = false) private short seatCapacity = 4;
    @Builder.Default @Column(name = "luggage_capacity", nullable = false) private short luggageCapacity = 0;
    @Builder.Default @Column(name = "price_per_seat", nullable = false, precision = 7, scale = 2) private BigDecimal pricePerSeat = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, length = 20) private String status = "ACTIVE";
    @Column(name = "origin_lat") private Double originLat;
    @Column(name = "origin_lng") private Double originLng;
    @Column(name = "dest_lat") private Double destLat;
    @Column(name = "dest_lng") private Double destLng;
    @Builder.Default @Column(name = "max_detour_km", nullable = false) private Double maxDetourKm = 5.0;
    @Builder.Default @Column(name = "allow_cross_community", nullable = false) private boolean allowCrossCommunity = false;
    @Builder.Default @Column(name = "auto_generate", nullable = false) private boolean autoGenerate = true;
    @Column(name = "last_generated_date") private LocalDate lastGeneratedDate;
    @Builder.Default @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Builder.Default @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
