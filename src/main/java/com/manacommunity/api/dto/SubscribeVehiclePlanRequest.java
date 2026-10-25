package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeVehiclePlanRequest {
    @NotNull
    private Long planCatalogId;

    @NotNull
    private Long vehicleId;

    private String paymentReference;
}
