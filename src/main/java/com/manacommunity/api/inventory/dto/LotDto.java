package com.manacommunity.api.inventory.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotDto {
    private Long id;
    private String name;
    private Long productId;
    private String productName;
    private Double productQty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
