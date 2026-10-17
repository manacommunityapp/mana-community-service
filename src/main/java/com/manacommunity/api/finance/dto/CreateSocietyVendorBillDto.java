package com.manacommunity.api.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSocietyVendorBillDto {

    private String vendorId;

    @NotNull
    private String vendorName;

    private String billNumber;

    private LocalDate billDate;

    private LocalDate dueDate;

    private String description;

    @NotNull
    private BigDecimal grossAmount;

    private BigDecimal gstPercent;

    private String tdsSection;

    private BigDecimal tdsRate;
}
