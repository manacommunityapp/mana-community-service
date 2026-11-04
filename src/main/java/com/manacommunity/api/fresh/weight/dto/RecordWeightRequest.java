package com.manacommunity.api.fresh.weight.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordWeightRequest {

    @NotNull(message = "Fulfilment item ID is required")
    private Long fulfilmentItemId;

    @NotNull(message = "Actual weight is required")
    @Positive(message = "Actual weight must be positive")
    private BigDecimal actualWeightG;

    private Long weighedByUserId;
    private String reason;
}
