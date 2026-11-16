package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VendorRegistrationResponse {
    private Long id;
    private String businessName;
    private Long categoryId;
    private String categoryName;
    private String businessType;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String description;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String gstNumber;
    private String panNumber;
    private Long applicantUserId;
    private String applicantUserName;
    private String applicantUserEmail;
    private String status;
    private String rejectionReason;
    private Long vendorId;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedById;
    private String reviewedByName;
}
