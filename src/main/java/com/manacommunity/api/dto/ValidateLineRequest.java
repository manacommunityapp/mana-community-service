package com.manacommunity.api.dto;

import java.math.BigDecimal;

public record ValidateLineRequest(
    Long productId,
    BigDecimal productQty,
    BigDecimal qtyDone
) {}
