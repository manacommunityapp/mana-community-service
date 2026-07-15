package com.manacommunity.api.dto;

import java.math.BigDecimal;

public record ScrapRequest(
    Long productId,
    Long locationId,
    BigDecimal quantity,
    String reason
) {}
