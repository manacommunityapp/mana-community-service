package com.manacommunity.api.vehicle.dto;

import lombok.Data;

@Data
public class RegisterVehicleRequest {
    private String plateNumber;
    private String vehicleType;
    private String make;
    private String model;
    private String color;
}
