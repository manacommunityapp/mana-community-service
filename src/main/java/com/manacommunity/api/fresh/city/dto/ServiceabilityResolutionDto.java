package com.manacommunity.api.fresh.city.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceabilityResolutionDto {
    private boolean isServiceable;
    private Long cityId;
    private String cityCode;
    private String cityName;
    private Long serviceZoneId;
    private String zoneCode;
    private String zoneName;
    private String postalCode;
    private String message;
}
