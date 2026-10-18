package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class MobilityVehicleDto {
    private Long id;
    private String vehicleType;
    private String make;
    private String model;
    private String numberPlate;
    private String color;
    private Integer year;
    private boolean active;
}
