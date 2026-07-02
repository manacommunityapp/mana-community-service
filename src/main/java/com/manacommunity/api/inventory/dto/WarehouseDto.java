package com.manacommunity.api.inventory.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDto {
    private Long id;
    private String name;
    private String code;
    private Long partnerId;
    private Long lotStockId;
    private String receptionSteps;
    private String deliverySteps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
