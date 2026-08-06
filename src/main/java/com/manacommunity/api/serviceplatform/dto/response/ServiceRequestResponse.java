package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ServiceRequestResponse {
    private Long id;
    private Long requesterId;
    private String requesterName;
    private Long categoryId;
    private String categoryName;
    private String domainName;
    private String title;
    private String description;
    private LocalDate preferredDate;
    private String preferredTimeSlot;
    private String address;
    private String urgency;
    private String status;
    private Long assignedProviderId;
    private String assignedProviderName;
    private Long assignedOfferingId;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private String customFieldValues;
    private String attachments;
    private String cancellationReason;
    private WorkOrderResponse workOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
