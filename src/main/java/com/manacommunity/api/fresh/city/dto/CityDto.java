package com.manacommunity.api.fresh.city.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityDto {
    private Long id;
    private String code;
    private String name;
    private String state;
    private String country;
    private String timezone;
    private Boolean isActive;
    private List<ServiceZoneDto> serviceZones;
}
