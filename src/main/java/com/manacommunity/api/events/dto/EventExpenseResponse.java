package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
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
