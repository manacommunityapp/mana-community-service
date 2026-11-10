package com.manacommunity.api.fresh.vendor.dto;

import com.manacommunity.api.fresh.vendor.model.FreshVendorServiceArea;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorServiceAreaDto {
    private Long id;
    private Long vendorId;
    private String vendorBusinessName;
    private Long communityId;
    private String communityName;
    private LocalTime cutoffTime;
    private String morningDeliverySlot;
    private Boolean isActive;

    public static VendorServiceAreaDto fromEntity(FreshVendorServiceArea entity) {
        if (entity == null) return null;
        return VendorServiceAreaDto.builder()
                .id(entity.getId())
                .vendorId(entity.getVendor() != null ? entity.getVendor().getId() : null)
                .vendorBusinessName(entity.getVendor() != null ? entity.getVendor().getBusinessName() : null)
                .communityId(entity.getCommunity() != null ? entity.getCommunity().getId() : null)
                .communityName(entity.getCommunity() != null ? entity.getCommunity().getName() : null)
                .cutoffTime(entity.getCutoffTime())
                .morningDeliverySlot(entity.getMorningDeliverySlot())
                .isActive(entity.getIsActive())
                .build();
    }
}
