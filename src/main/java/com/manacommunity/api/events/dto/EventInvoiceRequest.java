package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EventInvoiceRequest {

    @NotNull
    private Long eventId;

    @NotBlank
    private String vendorName;

    private String invoiceNumber;
    private String invoiceDate;   // ISO date string YYYY-MM-DD
    private String dueDate;       // ISO date string YYYY-MM-DD

    @NotNull
    private BigDecimal amount;

    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private String category;
    private String status;

    /** File URL — populated by the client after uploading via POST /api/files/upload. */
    private String invoiceUrl;

    /** DB file id (Postgres storage); null for S3. */
    private Long fileId;

    private String notes;
}
