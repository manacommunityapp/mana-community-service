package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.AmbulanceStatus;
import com.manacommunity.api.health.enums.HealthEnums.AmbulanceVehicleType;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_ambulance_booking", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthAmbulanceBooking extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "booking_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID bookingUuid = UUID.randomUUID();

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "patient_name", length = 120)
    private String patientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    @Builder.Default
    private AmbulanceVehicleType vehicleType = AmbulanceVehicleType.BASIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AmbulanceStatus status = AmbulanceStatus.REQUESTED;

    @Column(name = "pickup_address", length = 500)
    private String pickupAddress;

    @Column(name = "destination_address", length = 500)
    private String destinationAddress;

    @Column(name = "emergency_note", length = 1000)
    private String emergencyNote;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "driver_name", length = 100)
    private String driverName;

    @Column(name = "driver_phone", length = 20)
    private String driverPhone;

    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;
}
