package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class MobilityTripDriverDto {
    private Long driverId;
    private String driverName;
    private String phone;
    private String vehicleModel;
    private String vehicleNumber;
    private BigDecimal rating;
}
