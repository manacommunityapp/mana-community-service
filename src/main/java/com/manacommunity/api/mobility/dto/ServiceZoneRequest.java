package com.manacommunity.api.mobility.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceZoneRequest {
    @NotBlank @Size(max = 100) private String name;
    @NotNull private Double centerLat;
    @NotNull private Double centerLng;
    @NotNull @DecimalMin("0.5") @DecimalMax("100.0") private BigDecimal radiusKm;
    private Boolean active = Boolean.TRUE;
}
