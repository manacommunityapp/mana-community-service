package com.manacommunity.api.communityops.asset;

import java.time.LocalDate;

public record AssetDto(
    String name,
    AssetCategory category,
    String modelNumber,
    String serialNumber,
    String location,
    LocalDate purchaseDate,
    LocalDate warrantyExpiry,
    String amcVendor,
    LocalDate amcExpiry,
    String notes
) {}
