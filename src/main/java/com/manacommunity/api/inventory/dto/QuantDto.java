package com.manacommunity.api.inventory.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long locationId;
    private String locationCompleteName;
    private Double quantity;
    private Double reservedQuantity;
    private Long lotId;
    private String lotName;
    private LocalDateTime inDate;
    private LocalDateTime updatedAt;
}
