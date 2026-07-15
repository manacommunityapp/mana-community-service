package com.manacommunity.api.dto;

public record InventoryLocationResponse(
    Long id,
    String completeName,
    String usage,
    String barcode,
    Long warehouseId
) {}
