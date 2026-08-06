package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractRequest {
    @NotNull
    private Long vendorId;
    @NotBlank
    private String title;
    private String description;
    @NotBlank
    private String contractType;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    @NotNull
    private BigDecimal totalValue;
    private String paymentTerms;
    private Boolean autoRenew;
    private Integer renewalNoticeDays;
}
