package com.manacommunity.api.fresh.processing.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class LogTemperatureRequest {
    private BigDecimal loggedTempCelsius;
    private String locationTag;
    private Long loggedByUserId;
}