package com.manacommunity.api.freshkitchen.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.manacommunity.api.freshkitchen.model.VendorProductionCapacity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorCapacityDto {

    private Long id;

    @NotBlank(message = "vendorId is required")
    private String vendorId;

    private String vendorName;

    @NotBlank(message = "productId is required")
    private String productId;

    private String productName;

    @NotNull(message = "productionDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;

    @NotNull(message = "maxCapacity is required")
    @Min(value = 0, message = "maxCapacity cannot be negative")
    private Integer maxCapacity;

    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Boolean isSoldOut;
    private Long version;

    public static VendorCapacityDto fromEntity(VendorProductionCapacity entity) {
        if (entity == null) {
            return null;
        }
        return VendorCapacityDto.builder()
                .id(entity.getId())
                .vendorId(entity.getVendorId())
                .vendorName(entity.getVendorName())
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .productionDate(entity.getProductionDate())
                .maxCapacity(entity.getMaxCapacity())
                .reservedQuantity(entity.getReservedQuantity())
                .availableQuantity(entity.getAvailableQuantity())
                .isSoldOut(entity.getIsSoldOut())
                .version(entity.getVersion())
                .build();
    }
}
