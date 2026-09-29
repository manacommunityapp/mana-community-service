package com.manacommunity.api.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParkingSlotRequest(
        @NotBlank @Size(max = 20) String slotNumber,
        @Size(max = 40) String zone,
        @Size(max = 20) String floor,
        String slotType,
        String notes
) {}
