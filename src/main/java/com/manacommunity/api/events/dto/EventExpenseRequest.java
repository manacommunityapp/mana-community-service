package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventExpenseRequest {

    @NotNull
    private Long eventId;

    @NotBlank
    private String description;

    private String category;

    @NotNull
    private Double amount;

    private String vendorName;
    private String receiptUrl;
    private String expenseDate;
    private String status;
}
