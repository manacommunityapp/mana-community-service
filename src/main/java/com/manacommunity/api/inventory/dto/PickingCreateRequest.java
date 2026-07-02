package com.manacommunity.api.inventory.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickingCreateRequest {
    private String origin;
    private Long pickingTypeId;
    private LocalDateTime scheduledDate;
    private Long locationSrcId;
    private Long locationDestId;
    private List<LineRequest> lines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineRequest {
        private Long productId;
        private Double quantity;
        private String serialNumber; // optional serial/lot tracking
    }
}
