package com.manacommunity.api.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCarePlanResponse {
    private Long id;
    private String planName;
    private String frequencyLabel;
    private String vehicleNickname;
    private String vehiclePlate;
    private String status;
    private int creditsTotal;
    private int creditsUsed;
    private int creditsRemaining;
    private LocalDate startDate;
    private LocalDate endDate;
}
