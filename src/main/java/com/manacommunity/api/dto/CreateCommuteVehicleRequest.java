package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommuteVehicleRequest {
    @NotBlank
    private String vehicleType;
    private String model;
    private String color;
    @NotBlank
    private String numberPlate;
    private boolean isDefault;
}
