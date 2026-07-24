package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VendorResponse {
    private Long id;
    private String businessName;
    private String tradeName;
    private CategoryRef category;
    private String businessType;
    private String status;
    private String description;
    private String email;
    private String phone;
    private String alternatePhone;
    private String website;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String gstNumber;
    private String panNumber;
    private String logoUrl;
    private String bannerUrl;
    private BigDecimal commissionRate;
    private BigDecimal avgRating;
    private Integer totalRatings;
    private Boolean isVerified;
    private Long communityId;
    private List<VendorOwnerResponse> owners;
    private List<String> languages;
    private List<VendorBranchResponse> branches;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class CategoryRef {
        private Long id;
        private String name;
        private String icon;
    }
}
