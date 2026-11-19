package com.manacommunity.api.trip.p2p.dto;

import com.manacommunity.api.trip.p2p.entity.TripP2PCollectRequest;
import com.manacommunity.api.trip.p2p.entity.TripP2PCollectStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TripP2PDtos {

    @Data
    public static class CreateP2PDebtRequest {
        @NotNull
        private Long debtorUserId;

        @NotNull
        @DecimalMin(value = "1.00", message = "Amount must be at least ₹1")
        @Digits(integer = 12, fraction = 2)
        private BigDecimal amount;

        @Size(max = 255)
        private String description;
    }

    @Data
    public static class InitiateCollectRequest {
        @NotBlank
        @Size(max = 100)
        private String upiVpa;
    }

    @Data
    public static class TripP2PCollectResponse {
        private Long id;
        private String tripId;
        private Long communityId;
        private Long creditorUserId;
        private Long debtorUserId;
        private BigDecimal amount;
        private String upiVpa;
        private TripP2PCollectStatus status;
        private String razorpayPaymentId;
        private String description;
        private LocalDateTime initiatedAt;
        private LocalDateTime settledAt;
        private LocalDateTime createdAt;

        public static TripP2PCollectResponse from(TripP2PCollectRequest entity) {
            TripP2PCollectResponse dto = new TripP2PCollectResponse();
            dto.id = entity.getId();
            dto.tripId = entity.getTripId();
            dto.communityId = entity.getCommunityId();
            dto.creditorUserId = entity.getCreditorUserId();
            dto.debtorUserId = entity.getDebtorUserId();
            dto.amount = entity.getAmount();
            dto.upiVpa = entity.getUpiVpa();
            dto.status = entity.getStatus();
            dto.razorpayPaymentId = entity.getRazorpayPaymentId();
            dto.description = entity.getDescription();
            dto.initiatedAt = entity.getInitiatedAt();
            dto.settledAt = entity.getSettledAt();
            dto.createdAt = entity.getCreatedAt();
            return dto;
        }
    }
}
