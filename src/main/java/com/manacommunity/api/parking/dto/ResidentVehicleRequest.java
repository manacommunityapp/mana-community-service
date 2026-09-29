package com.manacommunity.api.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResidentVehicleRequest(
        String vehicleType,
        @Size(max = 60) String make,
        @Size(max = 60) String model,
        @Size(max = 30) String color,
        @NotBlank @Size(max = 20) String numberPlate,
        Long parkingSlotId,
        @Size(max = 30) String stickerNumber,
        boolean primary
) {}
