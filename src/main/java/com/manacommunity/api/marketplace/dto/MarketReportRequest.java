package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketReport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

public class MarketReportRequest {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotNull(message = "Listing ID is required")
        private Long listingId;

        @NotBlank(message = "Reason is required")
        @Size(max = 100)
        private String reason;

        @Size(max = 1000)
        private String details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Moderate {
        @NotNull(message = "Action is required")
        private MarketReport.ReportStatus status;

        @Size(max = 500)
        private String actionTaken;
    }
}
