package com.manacommunity.api.freshkitchen.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorDailyManifestDto {

    private String vendorId;
    private String vendorName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;

    private Integer totalMaxCapacity;
    private Integer totalReservedQuantity;
    private Integer totalAvailableQuantity;

    @Builder.Default
    private List<ManifestItemDto> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManifestItemDto {
        private String productId;
        private String productName;
        private Integer maxCapacity;
        private Integer reservedQuantity;
        private Integer availableQuantity;
        private Boolean isSoldOut;
        @Builder.Default
        private List<String> batchNumbers = new ArrayList<>();
    }
}
