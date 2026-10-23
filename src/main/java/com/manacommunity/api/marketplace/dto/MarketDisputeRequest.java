package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketDispute;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDisputeRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Reason is required")
    @Size(max = 100, message = "Reason must not exceed 100 characters")
    private String reason;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolveRequest {
        @NotNull(message = "Resolution status is required")
        private MarketDispute.DisputeStatus status;

        @NotBlank(message = "Resolution notes are required")
        private String resolutionNotes;
    }
}
