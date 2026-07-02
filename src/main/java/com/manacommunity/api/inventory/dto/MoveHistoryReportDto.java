package com.manacommunity.api.inventory.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveHistoryReportDto {
    private String transferReference;
    private String sourceDocument;
    private LocalDateTime executionDate;
    private String product;
    private String fromLocation;
    private String toLocation;
    private Double qtyDone;
    private String serialLotNumber;
}
