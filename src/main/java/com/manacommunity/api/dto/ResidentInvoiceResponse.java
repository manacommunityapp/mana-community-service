package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing an invoice response for residents in the billing module.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String residentName;
    private Long residentId;
    private String flatNo;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal totalAmount;
    private String dueDate;
    private String status;
    private String pdfUrl;
    private String eventTitle;
    private LocalDateTime generatedAt;
    private LocalDateTime paidAt;
}
