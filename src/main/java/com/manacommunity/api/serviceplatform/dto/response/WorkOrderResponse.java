package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkOrderResponse {
    private Long id;
    private Long serviceRequestId;
    private Long providerId;
    private String providerName;
    private String status;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private String notes;
    private String checklistItems;
    private String materialsUsed;
    private String beforePhotos;
    private String afterPhotos;
    private boolean residentSignoff;
    private LocalDateTime residentSignoffAt;
    private boolean providerSignoff;
    private LocalDateTime providerSignoffAt;
    private Long invoiceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
