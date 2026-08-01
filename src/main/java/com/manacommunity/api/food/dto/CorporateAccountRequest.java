package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CorporateAccountRequest {
    @NotBlank
    private String companyName;
    private String billingAddress;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private String gstNumber;
    private BigDecimal creditLimit;
}
