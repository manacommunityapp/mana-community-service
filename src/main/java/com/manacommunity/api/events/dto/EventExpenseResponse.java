package com.manacommunity.api.events.dto;

import lombok.Data;

@Data
public class EventExpenseResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String description;
    private String category;
    private Double amount;
    private String vendorName;
    private String receiptUrl;
    private String expenseDate;
    private String status;
    private String createdByName;
    private String createdAt;
}
