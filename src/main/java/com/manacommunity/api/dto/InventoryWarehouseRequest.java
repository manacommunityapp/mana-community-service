package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InventoryWarehouseRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 10) String code,
    String receptionSteps,   // ONE_STEP, TWO_STEPS, THREE_STEPS
    String deliverySteps
) {}
