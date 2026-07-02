package com.manacommunity.api.inventory.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveLineDto {
    private Long id;
    private Long productId;
    private String productName;
    private Long lotId;
    private String lotName;
    private Double quantity;
    private Long locationId;
    private String locationName;
    private Long locationDestId;
    private String locationDestName;
    private Long resultPackageId;
    private Long moveId;
    private Long pickingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
