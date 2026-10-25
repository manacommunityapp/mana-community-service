package com.manacommunity.api.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCareBookingResponse {
    private Long id;
    private String bookingNumber;
    private String status;

    // Vehicle
    private Long vehicleId;
    private String vehicleNickname;
    private String vehicleType;
    private String vehiclePlate;
    private String vehicleDisplay;

    // Service
    private Long serviceId;
    private String serviceName;
    private int durationMinutes;

    // Slot
    private Long slotId;
    private LocalDate scheduledDate;
    private LocalTime scheduledStart;
    private LocalTime scheduledEnd;

    // Technician
    private Long technicianId;
    private String technicianName;

    // Amounts
    private BigDecimal baseAmount;
    private BigDecimal addonAmount;
    private BigDecimal totalAmount;

    // Addons
    private List<VehicleServiceAddonResponse> addons;

    // Misc
    private String locationNotes;
    private Integer etaMinutes;
    private Integer rating;
    private String reviewText;

    private LocalDateTime createdAt;
}
