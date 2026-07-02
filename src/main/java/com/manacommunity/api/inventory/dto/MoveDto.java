package com.manacommunity.api.inventory.dto;

import com.manacommunity.api.inventory.entity.MoveState;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveDto {
    private Long id;
    private Long productId;
    private String productName;
    private Double productUomQty;
    private Double quantity;
    private MoveState state;
    private Long pickingId;
    private Long locationId;
    private String locationName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
