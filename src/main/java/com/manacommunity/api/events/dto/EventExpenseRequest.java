package com.manacommunity.api.events.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EventExpenseRequest {
    private Long eventId;
    private String description;
    private String category;
    private Double amount;
    private String vendorName;
    private String receiptUrl;
    private LocalDate expenseDate;
    private String status;
}
