package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class PendingDocumentReviewDto {
    private Long documentId;
    private Long driverId;
    private String driverName;
    private String docType;
    private String fileUrl;
    private Instant uploadedAt;
}
