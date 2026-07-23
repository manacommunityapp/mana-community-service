package com.manacommunity.api.booking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EquipmentResponse {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private Integer quantity;
    private String notes;
}
