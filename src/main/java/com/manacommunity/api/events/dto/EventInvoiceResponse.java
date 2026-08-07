package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EventInvoiceResponse {

    private Long id;
    private Long eventId;
    private String eventTitle;

    private String invoiceNumber;
    private String vendorName;
    private String invoiceDate;
    private String dueDate;

    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private String category;
    private String status;

    private String invoiceUrl;
    private Long fileId;
    private String notes;

    private String createdByName;
    private String createdAt;
    private String updatedAt;
}
