package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InventoryProductRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 50) String defaultCode,
    @Size(max = 100) String barcode,
    @PositiveOrZero BigDecimal listPrice,
    @PositiveOrZero BigDecimal standardPrice,
    String type,        // STORABLE, CONSUMABLE, SERVICE
    String tracking,    // NONE, LOT, SERIAL
    Long categId
) {}
