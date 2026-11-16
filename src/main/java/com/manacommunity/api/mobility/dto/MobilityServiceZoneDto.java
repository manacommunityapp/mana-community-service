package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class MobilityServiceZoneDto {
    private Long id;
    private String name;
    private Double centerLat;
    private Double centerLng;
    private BigDecimal radiusKm;
    private boolean active;
}
