package com.manacommunity.api.healthguard.entity;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.AmbulanceBookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ambulance_booking", schema = "manacommunity")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AmbulanceBooking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String bookingUuid;

    @Column(nullable = false)
    private Long userId;

    private Long incidentId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private AmbulanceBookingStatus status = AmbulanceBookingStatus.REQUESTED;

    private String pickupAddress;
    private String destinationAddress;
    private String patientName;
    private String patientPhone;
    private String emergencyDescription;

    private String vehicleNumber;
    private String driverName;
    private String driverPhone;
    private Integer estimatedArrivalMinutes;

    @Column(updatable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime confirmedAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime completedAt;
    private String notes;

    @PrePersist
    protected void onCreate() {
        bookingUuid = UUID.randomUUID().toString();
        requestedAt = LocalDateTime.now();
    }
}
