package com.manacommunity.api.mobility.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "mobility_trip", indexes = {
    @Index(name = "idx_mob_trip_passenger", columnList = "passenger_id, requested_at DESC"),
    @Index(name = "idx_mob_trip_driver",    columnList = "driver_id, requested_at DESC"),
    @Index(name = "idx_mob_trip_status",    columnList = "status"),
    @Index(name = "idx_mob_trip_community", columnList = "community_id, requested_at DESC")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MobilityTrip {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_ref", nullable = false, unique = true, length = 16)
    private String tripRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private AppUser passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private MobilityDriver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private MobilityVehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    private MobilityServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private MobilityTripStatus status = MobilityTripStatus.REQUESTED;

    @Column(name = "pickup_lat", nullable = false)
    private double pickupLat;

    @Column(name = "pickup_lng", nullable = false)
    private double pickupLng;

    @Column(name = "pickup_address", nullable = false, columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "dest_lat", nullable = false)
    private double destLat;

    @Column(name = "dest_lng", nullable = false)
    private double destLng;

    @Column(name = "dest_address", nullable = false, columnDefinition = "TEXT")
    private String destAddress;

    @Column(name = "pickup_pin", length = 6)
    private String pickupPin;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "quote_id", length = 64)
    private String quoteId;

    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare = BigDecimal.ZERO;

    @Column(name = "distance_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceFare = BigDecimal.ZERO;

    @Column(name = "duration_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal durationFare = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxes = BigDecimal.ZERO;

    @Column(name = "quoted_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal quotedTotal = BigDecimal.ZERO;

    @Column(name = "final_fare", precision = 10, scale = 2)
    private BigDecimal finalFare;

    @Column(name = "distance_km", precision = 7, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod = "CASH";

    @Column(name = "promo_code", length = 30)
    private String promoCode;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "passenger_rating")
    private Short passengerRating;

    @Column(name = "driver_rating")
    private Short driverRating;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "assigned_at")   private Instant assignedAt;
    @Column(name = "arrived_at")    private Instant arrivedAt;
    @Column(name = "picked_up_at")  private Instant pickedUpAt;
    @Column(name = "completed_at")  private Instant completedAt;
    @Column(name = "cancelled_at")  private Instant cancelledAt;
}
