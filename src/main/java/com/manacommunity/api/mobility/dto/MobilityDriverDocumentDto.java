package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class MobilityDriverDocumentDto {
    private Long id;
    private String docType;
    private String fileUrl;
    private String status;
    private String rejectReason;
    private Instant uploadedAt;
    private Instant reviewedAt;
}
