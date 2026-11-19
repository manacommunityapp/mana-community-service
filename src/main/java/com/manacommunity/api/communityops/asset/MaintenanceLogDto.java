package com.manacommunity.api.communityops.asset;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaintenanceLogDto(
    Long scheduleId,
    String taskDescription,
    MaintenanceLogStatus status,
    LocalDate scheduledDate,
    LocalDate completedDate,
    String performedBy,
    BigDecimal costActual,
    String notes
) {}
