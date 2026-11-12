package com.manacommunity.api.freshkitchen.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchReleaseRequest {

    @NotNull(message = "storageTempCelsius is required for quality gate verification")
    private BigDecimal storageTempCelsius;

    private String releasedBy;

    private String notes;
}
