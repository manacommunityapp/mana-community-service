package com.manacommunity.api.homeservices.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompleteBookingRequest {
    private Integer rating;
    private String review;
    private BigDecimal amount;
}
