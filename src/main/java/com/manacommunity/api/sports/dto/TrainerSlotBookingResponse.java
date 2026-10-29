package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TrainerSlotBookingResponse {
    private Long id;
    private Long slotId;
    private Long trainerId;
    private Long userId;
    private String userName;
    private String tower;
    private String flatNumber;
    private String status;
    private BigDecimal amountPaid;
    private LocalDateTime bookedAt;
    // Flattened slot info for convenience
    private TrainerSlotResponse slot;
}
