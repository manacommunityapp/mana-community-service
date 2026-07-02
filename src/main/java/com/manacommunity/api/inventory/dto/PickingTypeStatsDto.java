package com.manacommunity.api.inventory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickingTypeStatsDto {
    private Long pickingTypeId;
    private String operationType;
    private String warehouse;
    private Long toProcess;
    private Long lateTransfers;
    private Long backorders;
}
