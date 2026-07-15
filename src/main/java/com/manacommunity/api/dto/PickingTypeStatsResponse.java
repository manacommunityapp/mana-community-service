package com.manacommunity.api.dto;

public record PickingTypeStatsResponse(
    Long id,
    String name,
    String code,
    Long warehouseId,
    long toProcessCount,
    long lateCount,
    long backorderCount
) {}
