package com.manacommunity.api.dto;

import java.math.BigDecimal;

public record MoveLineRequest(
    Long productId,
    BigDecimal productQty,
    BigDecimal qtyDone
) {}
