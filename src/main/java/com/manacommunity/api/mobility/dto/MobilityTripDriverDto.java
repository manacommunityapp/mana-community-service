package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder
public class MobilityTripDriverDto {
    private Long driverId;
    private String driverName;
    private String phone;
    private String vehicleModel;
    private String vehicleNumber;
    private BigDecimal rating;
    private Double currentLat;
    private Double currentLng;
    private Instant locationUpdatedAt;
}
