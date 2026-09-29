package com.manacommunity.api.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveSpotRequest {

    @NotNull(message = "Spot ID is required")
    private Long spotId;

    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;

    private String vehicleType;

    private String notes;
}
