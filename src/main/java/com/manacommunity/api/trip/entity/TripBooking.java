package com.manacommunity.api.trip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_trip_booking", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripBooking {
    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 50)
    private String tripId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "trip_title", nullable = false, length = 200)
    private String tripTitle;

    @Column(nullable = false, length = 200)
    private String destination;

    @Column(name = "departure_date", nullable = false, length = 100)
    private String departureDate;

    @Column(name = "participant_count", nullable = false)
    @Builder.Default
    private Integer participantCount = 1;

    @Column(name = "passengers_json", columnDefinition = "TEXT")
    private String passengersJson;

    @Column(name = "selected_pickup_point", length = 200)
    private String selectedPickupPoint;

    @Column(name = "selected_room_type", length = 100)
    private String selectedRoomType;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "CONFIRMED";

    @Column(name = "boarding_pass_qr", length = 150)
    private String boardingPassQR;

    @Column(name = "host_name", length = 150)
    private String host;

    @Column(name = "booked_at", nullable = false)
    @Builder.Default
    private LocalDateTime bookedAt = LocalDateTime.now();

    @Column(name = "checked_in", nullable = false)
    @Builder.Default
    private Boolean checkedIn = false;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;
}