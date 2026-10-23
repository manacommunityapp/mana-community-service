package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data @Builder
public class MobilityDriverDto {
    private Long id;
    private Long userId;
    private String driverName;
    private String status;
    private String availability;
    private BigDecimal avgRating;
    private int totalTrips;
    private Instant createdAt;
    private List<MobilityVehicleDto> vehicles;
}
