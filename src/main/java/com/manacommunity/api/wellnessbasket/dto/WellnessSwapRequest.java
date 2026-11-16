package com.manacommunity.api.wellnessbasket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessSwapRequest {

    @NotNull(message = "Delivery date is required")
    private LocalDate deliveryDate;

    @NotNull(message = "Original product ID is required")
    private Long originalProductId;

    @NotNull(message = "Requested replacement product ID is required")
    private Long requestedProductId;

    private String reason;
}
