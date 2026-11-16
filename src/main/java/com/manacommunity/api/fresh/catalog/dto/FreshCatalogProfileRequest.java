package com.manacommunity.api.fresh.catalog.dto;

import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class FreshCatalogProfileRequest {
    private Long vendorProductId;
    private FreshVendorCapabilityType capabilityType;
    private Integer shelfLifeHours;
    private String storageInstruction;
    private String originState;
    private String originDistrict;
    private Boolean isOrganic;
    private String organicCertNumber;
    private Boolean isPesticideFree;
    private Boolean isAntibioticFree;
    private Boolean isHormoneFree;
    private BigDecimal minOrderQty;
    private BigDecimal maxOrderQty;
    private String orderUnit;
}