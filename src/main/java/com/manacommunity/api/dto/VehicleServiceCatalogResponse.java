package com.manacommunity.api.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleServiceCatalogResponse {
    private Long id;
    private String name;
    private String description;
    private List<String> vehicleTypes;
    private int durationMinutes;
    private BigDecimal basePrice;
    private BigDecimal communityPrice;
    private boolean isPopular;
    private String gradientStart;
    private String gradientEnd;
    private List<VehicleServiceAddonResponse> addons;
}
