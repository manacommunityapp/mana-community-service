package com.manacommunity.api.fresh.processing.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class RecordOutputRequest {
    private BigDecimal actualOutputQty;
    private BigDecimal wastageQty;
    private String wastageReason;
    private BigDecimal storageTempCelsius;
}