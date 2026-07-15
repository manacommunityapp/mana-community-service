package com.manacommunity.api.dto;

public record InventoryWarehouseResponse(
    Long id,
    String name,
    String code,
    Long partnerId,
    Long lotStockId,
    String receptionSteps,
    String deliverySteps
) {}
