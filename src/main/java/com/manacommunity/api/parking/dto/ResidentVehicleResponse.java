package com.manacommunity.api.parking.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ResidentVehicleResponse(
        Long id,
        Long ownerId,
        String ownerName,
        String vehicleType,
        String make,
        String model,
        String color,
        String numberPlate,
        Long parkingSlotId,
        String parkingSlotNumber,
        String stickerNumber,
        boolean primary,
        String status,
        LocalDateTime createdAt
) {}
