package com.manacommunity.api.fresh.vendor.dto;

import com.manacommunity.api.fresh.vendor.model.FreshVendorStaff;
import com.manacommunity.api.fresh.vendor.model.VendorStaffRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorStaffDto {
    private Long id;
    private Long vendorId;
    private String vendorBusinessName;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userPhone;
    private VendorStaffRole vendorRole;
    private Boolean isActive;
    private Long invitedByUserId;
    private LocalDateTime createdAt;

    public static VendorStaffDto fromEntity(FreshVendorStaff entity) {
        if (entity == null) return null;
        return VendorStaffDto.builder()
                .id(entity.getId())
                .vendorId(entity.getVendor() != null ? entity.getVendor().getId() : null)
                .vendorBusinessName(entity.getVendor() != null ? entity.getVendor().getBusinessName() : null)
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userFullName(entity.getUser() != null ? entity.getUser().getFullName() : null)
                .userEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .userPhone(entity.getUser() != null ? entity.getUser().getPhone() : null)
                .vendorRole(entity.getVendorRole())
                .isActive(entity.getIsActive())
                .invitedByUserId(entity.getInvitedByUser() != null ? entity.getInvitedByUser().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
