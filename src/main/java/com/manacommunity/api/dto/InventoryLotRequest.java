package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record InventoryLotRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull Long productId,
    LocalDate expirationDate,
    String notes
) {}
