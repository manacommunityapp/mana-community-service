package com.manacommunity.api.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleProfileResponse {
    private Long id;
    private String nickname;
    private String vehicleType;
    private String make;
    private String model;
    private String color;
    private String plateNumber;
    private boolean isDefault;
    private LocalDateTime createdAt;
}
