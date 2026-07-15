package com.manacommunity.api.dto;

import java.math.BigDecimal;

public record MoveLineResponse(
    Long id,
    Long productId,
    String productName,
    BigDecimal productQty,
    BigDecimal qtyDone
) {}
