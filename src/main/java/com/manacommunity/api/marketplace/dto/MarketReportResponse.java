package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketReport;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketReportResponse {

    private Long id;
    private Long listingId;
    private String listingTitle;
    private Long reporterId;
    private String reporterName;
    private Long communityId;
    private String reason;
    private String details;
    private MarketReport.ReportStatus status;
    private String actionTaken;
    private String moderatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
