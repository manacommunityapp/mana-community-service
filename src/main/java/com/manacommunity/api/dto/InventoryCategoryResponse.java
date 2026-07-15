package com.manacommunity.api.dto;

public record InventoryCategoryResponse(
    Long id,
    String name,
    Long parentId
) {}
