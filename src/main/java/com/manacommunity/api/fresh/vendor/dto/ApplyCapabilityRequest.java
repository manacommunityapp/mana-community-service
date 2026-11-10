package com.manacommunity.api.fresh.vendor.dto;

import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCapabilityRequest {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotNull(message = "Capability type is required")
    private FreshVendorCapabilityType capabilityType;

    private String fssaiLicenseNumber;
    private LocalDate fssaiValidUntil;
    private String facilityAddress;
    private Boolean coldStorageAvailable;
    private Integer dailyCapacityUnits;
    private String operatingDays;
}
