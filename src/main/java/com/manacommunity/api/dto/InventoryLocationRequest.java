package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InventoryLocationRequest(
    @NotBlank @Size(max = 200) String completeName,
    @NotBlank String usage,  // VIEW, INTERNAL, CUSTOMER, VENDOR, etc.
    @Size(max = 100) String barcode,
    Long warehouseId
) {}
