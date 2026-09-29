package com.manacommunity.api.parking.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpotDto {
    private Long id;
    private String spotNumber;
    private String level;
    private String type; // CAR, BIKE, EV
    private String status; // AVAILABLE, OCCUPIED, RESERVED
    private String vehicleNumber;
    private String ownerName;
    private String ownerFlat;
    private Long assignedUserId;
    private Long communityId;
    private String notes;
}
