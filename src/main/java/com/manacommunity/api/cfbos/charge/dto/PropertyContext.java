package com.manacommunity.api.cfbos.charge.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyContext {
    private BigDecimal area;
    private Integer floorNumber;
    private Integer occupants;
    private String propertyType;
    private BigDecimal consumption;
    private Boolean hasParking;
    private String parkingType;
    private Integer parkingSlots;
}
