package com.manacommunity.api.fresh.vendor.dto;

import com.manacommunity.api.fresh.vendor.model.CapabilityStatus;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapability;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorCapabilityDto {
    private Long id;
    private Long vendorId;
    private String vendorBusinessName;
    private FreshVendorCapabilityType capabilityType;
    private CapabilityStatus status;
    private Long approvedByUserId;
    private String approvedByUserName;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private String fssaiLicenseNumber;
    private LocalDate fssaiValidUntil;
    private String facilityAddress;
    private Boolean coldStorageAvailable;
    private Integer dailyCapacityUnits;
    private String operatingDays;
    private LocalDateTime createdAt;

    public static VendorCapabilityDto fromEntity(FreshVendorCapability entity) {
        if (entity == null) return null;
        return VendorCapabilityDto.builder()
                .id(entity.getId())
                .vendorId(entity.getVendor() != null ? entity.getVendor().getId() : null)
                .vendorBusinessName(entity.getVendor() != null ? entity.getVendor().getBusinessName() : null)
                .capabilityType(entity.getCapabilityType())
                .status(entity.getStatus())
                .approvedByUserId(entity.getApprovedByUser() != null ? entity.getApprovedByUser().getId() : null)
                .approvedByUserName(entity.getApprovedByUser() != null ? entity.getApprovedByUser().getFullName() : null)
                .approvedAt(entity.getApprovedAt())
                .rejectionReason(entity.getRejectionReason())
                .fssaiLicenseNumber(entity.getFssaiLicenseNumber())
                .fssaiValidUntil(entity.getFssaiValidUntil())
                .facilityAddress(entity.getFacilityAddress())
                .coldStorageAvailable(entity.getColdStorageAvailable())
                .dailyCapacityUnits(entity.getDailyCapacityUnits())
                .operatingDays(entity.getOperatingDays())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
