package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ContractResponse {
    private Long id;
    private String contractNumber;
    private VendorRef vendor;
    private String title;
    private String description;
    private String contractType;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalValue;
    private String paymentTerms;
    private Boolean autoRenew;
    private Integer renewalNoticeDays;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class VendorRef {
        private Long id;
        private String businessName;
    }
}
