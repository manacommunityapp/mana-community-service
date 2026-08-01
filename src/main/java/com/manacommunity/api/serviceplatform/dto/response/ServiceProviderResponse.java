package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ServiceProviderResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long vendorId;
    private String providerType;
    private String businessName;
    private String phone;
    private String email;
    private String bio;
    private String profileImageUrl;
    private String verificationStatus;
    private BigDecimal avgRating;
    private Integer totalJobsCompleted;
    private String serviceAreas;
    private String certifications;
    private List<ServiceOfferingResponse> offerings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
