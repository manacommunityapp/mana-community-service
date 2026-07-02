package com.manacommunity.api.inventory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLevelReportDto {
    private String warehouse;
    private String location;
    private String productName;
    private String internalReference; // SKU
    private Double onHand;
    private Double reserved;
    private Double available;
}
