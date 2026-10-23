package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Response body for a successful slot reservation. */
@Data
@Builder
public class PoojaReserveResponse {
    private Long reservationId;
    private Long scheduleId;
    private String idempotencyKey;
    private int reservedFamilyCount;
    private int reservedDevoteeCount;
    private LocalDateTime expiresAt;
    private String status;
    /** Santalpam token number assigned for this slot (= nextTokenSeq at reservation time). */
    private int tokenNumber;
}
