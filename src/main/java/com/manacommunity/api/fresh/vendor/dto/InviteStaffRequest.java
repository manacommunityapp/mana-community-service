package com.manacommunity.api.fresh.vendor.dto;

import com.manacommunity.api.fresh.vendor.model.VendorStaffRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteStaffRequest {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Vendor role is required")
    private VendorStaffRole vendorRole;
}
