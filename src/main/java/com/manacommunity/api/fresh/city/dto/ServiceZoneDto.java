package com.manacommunity.api.fresh.city.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceZoneDto {
    private Long id;
    private Long cityId;
    private String cityName;
    private String name;
    private String code;
    private Boolean isActive;
    private LocalTime deliveryCutoffTime;
    private List<String> postalCodes;
}
