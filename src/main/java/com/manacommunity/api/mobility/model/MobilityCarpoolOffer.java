package com.manacommunity.api.mobility.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "mobility_carpool_offer", indexes = {
    @Index(name = "idx_mob_carpool_community", columnList = "community_id, departure_time")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MobilityCarpoolOffer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_user_id", nullable = false)
    private AppUser driverUser;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String origin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String destination;

    @Column(name = "journey_type", nullable = false, length = 30)
    private String journeyType = "GENERIC";

    @Column(name = "journey_direction", length = 20)
    private String journeyDirection;

    @Column(name = "hub_code", length = 30)
    private String hubCode;

    @Column(name = "context_name", length = 160)
    private String contextName;

    @Column(name = "airport_direction", length = 20)
    private String airportDirection;

    @Column(name = "airport_code", length = 3)
    private String airportCode;

    @Column(name = "airport_name", length = 160)
    private String airportName;

    @Column(name = "flight_number", length = 20)
    private String flightNumber;

    @Column(name = "terminal", length = 20)
    private String terminal;

    @Column(name = "luggage_capacity")
    private Short luggageCapacity;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "total_seats", nullable = false)
    private short totalSeats = 3;

    @Column(name = "available_seats", nullable = false)
    private short availableSeats = 3;

    @Column(name = "price_per_seat", nullable = false, precision = 7, scale = 2)
    private BigDecimal pricePerSeat = BigDecimal.ZERO;

    @Column(name = "vehicle_model", length = 80)
    private String vehicleModel;

    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "driver_rating", precision = 3, scale = 2)
    private BigDecimal driverRating = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "origin_lat")
    private Double originLat;

    @Column(name = "origin_lng")
    private Double originLng;

    @Column(name = "dest_lat")
    private Double destLat;

    @Column(name = "dest_lng")
    private Double destLng;

    @Builder.Default
    @Column(name = "max_detour_km", nullable = false)
    private Double maxDetourKm = 5.0;

    @Builder.Default
    @Column(name = "allow_cross_community", nullable = false)
    private boolean allowCrossCommunity = false;

    @Column(name = "source_slot_id")
    private Long sourceSlotId;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Builder.Default
    @Column(name = "parent_consent_required", nullable = false)
    private boolean parentConsentRequired = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
