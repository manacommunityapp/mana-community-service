package com.manacommunity.api.mobility.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
