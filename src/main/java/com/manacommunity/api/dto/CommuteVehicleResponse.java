package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommuteVehicleResponse {
    private Long id;
    private String vehicleType;
    private String model;
    private String color;
    private String numberPlate;
    private boolean isDefault;
    private LocalDateTime createdAt;
}
