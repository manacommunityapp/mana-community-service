package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CorporateAccountResponse {
    private Long id;
    private String companyName;
    private String billingAddress;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private String gstNumber;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private String status;
    private Integer cardCount;
    private Integer cafeteriaCount;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
