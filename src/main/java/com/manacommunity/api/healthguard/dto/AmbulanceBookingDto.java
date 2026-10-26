package com.manacommunity.api.healthguard.dto;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.AmbulanceBookingStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AmbulanceBookingDto {
    private Long id;
    private String bookingUuid;
    private Long userId;
    private Long incidentId;
    private AmbulanceBookingStatus status;
    private String pickupAddress;
    private String destinationAddress;
    private String patientName;
    private String patientPhone;
    private String emergencyDescription;
    private String vehicleNumber;
    private String driverName;
    private String driverPhone;
    private Integer estimatedArrivalMinutes;
    private LocalDateTime requestedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime arrivedAt;
    private String notes;
}
