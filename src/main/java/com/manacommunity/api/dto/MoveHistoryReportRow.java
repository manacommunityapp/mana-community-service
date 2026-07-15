package com.manacommunity.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MoveHistoryReportRow(
    Long moveLineId,
    LocalDateTime date,
    String reference,
    String productName,
    String sourceLocation,
    String destLocation,
    BigDecimal qtyDone
) {}
