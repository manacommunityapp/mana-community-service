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
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "last_match_notified_at") private Instant lastMatchNotifiedAt;
}
