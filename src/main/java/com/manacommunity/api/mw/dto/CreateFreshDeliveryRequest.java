package com.manacommunity.api.mw.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CreateFreshDeliveryRequest {
    @NotBlank
    private String freshOrderId;

    @NotNull
    private Long employeeId;

    private String pickupLocation;
    private String dropLocation;

    @DecimalMin("0.0")
    private BigDecimal estimatedKm;

    private String assignmentType;
}
