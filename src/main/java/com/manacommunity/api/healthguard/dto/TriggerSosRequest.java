package com.manacommunity.api.healthguard.dto;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.AlertTriggerType;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.IncidentSeverity;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerSosRequest {
    private String description;
    private AlertTriggerType triggerType;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationAddress;
    private IncidentSeverity severity;
}
