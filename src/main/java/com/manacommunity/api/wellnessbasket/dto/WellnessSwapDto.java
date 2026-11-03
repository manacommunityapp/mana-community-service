package com.manacommunity.api.wellnessbasket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessSwapDto {
    private Long id;
    private Long subscriberPreferenceId;
    private Long userId;
    private LocalDate deliveryDate;
    private Long originalProductId;
    private String originalProductName;
    private Long requestedProductId;
    private String requestedProductName;
    private String status; // PENDING, APPROVED, REJECTED
    private String rejectionReason;
    private OffsetDateTime requestedAt;
    private OffsetDateTime resolvedAt;
}
