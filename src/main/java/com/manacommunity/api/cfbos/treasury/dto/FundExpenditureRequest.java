package com.manacommunity.api.cfbos.treasury.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FundExpenditureRequest {
    @NotNull
    private Long fundAccountId;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    private String approvedByResolutionNo;

    private Long vendorId;
    private String vendorName;
    private String invoiceNumber;
    private String quotationAttachmentUrl;
}
