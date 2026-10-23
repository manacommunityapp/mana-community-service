package com.manacommunity.api.healthguard.dto;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.WearableEventType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WearableHealthEventDto {
    private Long id;
    private Long userId;
    private Long deviceId;
    private WearableEventType eventType;
    private Integer heartRateBpm;
    private BigDecimal spo2Percent;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String rawData;
    private Boolean sosTriggered;
    private Long linkedIncidentId;
    private LocalDateTime eventAt;
    private LocalDateTime createdAt;
}
