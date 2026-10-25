package com.manacommunity.api.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleServiceAddonResponse {
    private Long id;
    private String name;
    private BigDecimal price;
}
