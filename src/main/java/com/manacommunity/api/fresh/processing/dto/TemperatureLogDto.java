package com.manacommunity.api.fresh.processing.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder
public class TemperatureLogDto {
    private Long id;
    private Long batchId;
    private BigDecimal loggedTempCelsius;
    private String locationTag;
    private Long loggedByUserId;
    private Instant loggedAt;
}