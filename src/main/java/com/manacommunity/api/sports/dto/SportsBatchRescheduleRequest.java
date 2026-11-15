package com.manacommunity.api.sports.dto;

import java.util.List;

public record SportsBatchRescheduleRequest(
    Long configId,
    String mode,
    Integer delayMinutes,
    String newDate,
    String startTime,
    Integer intervalMinutes,
    String fromDateTime,
    List<Long> matchIds
) {}
