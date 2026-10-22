package com.manacommunity.api.healthguard.dto;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.AlertTriggerType;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.IncidentSeverity;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.IncidentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyIncidentDto {
    private Long id;
    private String incidentUuid;
    private Long userId;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationAddress;
    private String description;
    private AlertTriggerType triggerType;
    private LocalDateTime triggeredAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private String notes;
    private Integer alertsSent;
    private List<EmergencyNotificationDto> notifications;
}
