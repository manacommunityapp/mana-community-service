package com.manacommunity.api.safety.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnprEventResponse {
    private Long id;
    private String plateNumber;
    private String vehicleType;
    private String gate;
    private String direction;
    private LocalDateTime timestamp;
    private String imageUrl;
    private Boolean recognized;
    private String ownerName;
    private String ownerFlat;
    private String alertType;
    private LocalDateTime createdAt;
}
