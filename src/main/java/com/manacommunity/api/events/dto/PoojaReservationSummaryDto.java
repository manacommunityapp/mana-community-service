package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Read-only summary of a single reservation slot — returned to admins. */
@Data
@Builder
public class PoojaReservationSummaryDto {
    private Long id;
    private Long scheduleId;
    private Long userId;
    private String userDisplayName;
    private Integer reservedFamilyCount;
    private Integer reservedDevoteeCount;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Long registrationId;
    private Integer tokenNumber;
}
