package com.manacommunity.api.mobility.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "mobility_airport_ride_request", indexes = {
        @Index(name = "idx_airport_request_match", columnList = "community_id, airport_code, direction, travel_at, status"),
        @Index(name = "idx_airport_request_user", columnList = "requester_id, created_at DESC")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MobilityAirportRideRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "requester_id", nullable = false)
    private AppUser requester;

    @Column(nullable = false, length = 20) private String direction;
    @Column(name = "journey_type", nullable = false, length = 30) private String journeyType = "AIRPORT";
    @Column(name = "airport_code", length = 3) private String airportCode;
    @Column(name = "hub_code", length = 30) private String hubCode;
    @Column(name = "context_name", length = 160) private String contextName;
    @Column(nullable = false, columnDefinition = "TEXT") private String area;
    @Column(name = "travel_at", nullable = false) private Instant travelAt;
    @Column(name = "time_window_minutes", nullable = false) private int timeWindowMinutes = 30;
    @Column(nullable = false) private short seats = 1;
    @Column(name = "luggage_count", nullable = false) private short luggageCount = 0;
    @Column(name = "flight_number", length = 20) private String flightNumber;
    @Column(length = 20) private String terminal;
    @Column(nullable = false, length = 20) private String status = "OPEN";
    @Column(name = "pickup_lat") private Double pickupLat;
    @Column(name = "pickup_lng") private Double pickupLng;
    @Column(name = "dest_lat") private Double destLat;
    @Column(name = "dest_lng") private Double destLng;
    @Builder.Default @Column(name = "max_detour_km", nullable = false) private Double maxDetourKm = 5.0;
    @Builder.Default @Column(name = "allow_cross_community", nullable = false) private boolean allowCrossCommunity = false;
    @Builder.Default @Column(name = "auto_reserve", nullable = false) private boolean autoReserve = false;
    @Builder.Default @Column(name = "parent_consent_required", nullable = false) private boolean parentConsentRequired = false;
    @Column(name = "guardian_name", length = 120) private String guardianName;
    @Column(name = "guardian_phone", length = 25) private String guardianPhone;
    @Builder.Default @Column(name = "guardian_consent_status", nullable = false, length = 20) private String guardianConsentStatus = "NOT_REQUIRED";
    @Column(name = "guardian_consented_at") private Instant guardianConsentedAt;
    @Column(name = "guardian_approved_by") private Long guardianApprovedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "last_match_notified_at") private Instant lastMatchNotifiedAt;
}
