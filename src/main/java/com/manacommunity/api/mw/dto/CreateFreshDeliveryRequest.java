package com.manacommunity.api.mw.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CreateFreshDeliveryRequest {
    private String freshOrderId;
    private Long employeeId;
    private String pickupLocation;
    private String dropLocation;
    private BigDecimal estimatedKm;
    private String assignmentType;
}
