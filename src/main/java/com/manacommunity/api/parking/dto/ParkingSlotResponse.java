package com.manacommunity.api.parking.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ParkingSlotResponse(
        Long id,
        String slotNumber,
        String zone,
        String floor,
        String slotType,
        String status,
        Long assignedToId,
        String assignedToName,
        Long vehicleId,
        String vehicleNumberPlate,
        String notes,
        LocalDateTime createdAt
) {}
