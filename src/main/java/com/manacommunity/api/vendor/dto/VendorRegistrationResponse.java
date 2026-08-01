package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VendorRegistrationResponse {
    private Long id;
    private String businessName;
    private String categoryName;
    private String businessType;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String status;
    private String rejectionReason;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
