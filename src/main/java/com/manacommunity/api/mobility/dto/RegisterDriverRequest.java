package com.manacommunity.api.mobility.dto;

import com.manacommunity.api.mobility.model.MobilityVehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterDriverRequest {
    @NotNull  private MobilityVehicleType vehicleType;
    @NotBlank private String vehicleModel;
    @NotBlank private String vehicleNumberPlate;
              private String vehicleColor;
              private Integer vehicleYear;
}
