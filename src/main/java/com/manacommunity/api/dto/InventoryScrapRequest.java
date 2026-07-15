package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InventoryScrapRequest(
    @NotNull Long productId,
    Long lotId,
    @NotNull Long locationId,
    @NotNull @Positive BigDecimal quantity,
    @Size(max = 500) String reason
) {}
