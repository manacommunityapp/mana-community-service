package com.manacommunity.api.dto;

import java.math.BigDecimal;

public record InventoryProductResponse(
    Long id,
    String name,
    String defaultCode,
    String barcode,
    BigDecimal listPrice,
    BigDecimal standardPrice,
    String type,
    String tracking,
    Long categId,
    BigDecimal qtyAvailable
) {}
