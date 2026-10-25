package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVehicleProfileRequest {
    @NotBlank
    @Size(max = 80)
    private String nickname;

    @NotBlank
    private String vehicleType;

    @NotBlank
    @Size(max = 50)
    private String make;

    @NotBlank
    @Size(max = 80)
    private String model;

    @Size(max = 30)
    private String color;

    @NotBlank
    @Size(max = 20)
    private String plateNumber;

    private boolean setAsDefault;
}
