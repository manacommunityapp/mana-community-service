package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "commute_booking", indexes = {
    @Index(name = "idx_booking_ride", columnList = "ride_id"),
    @Index(name = "idx_booking_passenger", columnList = "passenger_id"),
    @Index(name = "idx_booking_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_ride_passenger", columnNames = {"ride_id", "passenger_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommuteBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private CommuteRide ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private AppUser passenger;

    @Column(name = "seats_booked", nullable = false)
    @Builder.Default
    private int seatsBooked = 1;

    @Column(name = "pickup_note", length = 255)
    private String pickupNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CommuteBookingStatus status = CommuteBookingStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
