package com.manacommunity.api.inventory.dto;

import com.manacommunity.api.inventory.entity.LocationUsage;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private Long id;
    private String completeName;
    private LocationUsage usage;
    private Long locationId;
    private String barcode;
    private Long warehouseId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
