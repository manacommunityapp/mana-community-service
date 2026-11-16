package com.manacommunity.api.freshkitchen.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.manacommunity.api.freshkitchen.model.BatchStatus;
import com.manacommunity.api.freshkitchen.model.FoodProductionBatch;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionBatchDto {

    private Long id;
    private String batchNumber;
    private String productId;
    private String productName;
    private String vendorId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;

    private Integer batchQuantity;
    private BigDecimal storageTempCelsius;
    private BatchStatus batchStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiryDateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime releasedAt;

    private String releasedBy;
    private String notes;

    public static ProductionBatchDto fromEntity(FoodProductionBatch entity) {
        if (entity == null) {
            return null;
        }
        return ProductionBatchDto.builder()
                .id(entity.getId())
                .batchNumber(entity.getBatchNumber())
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .vendorId(entity.getVendorId())
                .productionDate(entity.getProductionDate())
                .batchQuantity(entity.getBatchQuantity())
                .storageTempCelsius(entity.getStorageTempCelsius())
                .batchStatus(entity.getBatchStatus())
                .expiryDateTime(entity.getExpiryDateTime())
                .releasedAt(entity.getReleasedAt())
                .releasedBy(entity.getReleasedBy())
                .notes(entity.getNotes())
                .build();
    }
}
