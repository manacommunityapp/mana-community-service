package com.manacommunity.api.homeservices.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookServiceRequest {
    private Long providerId;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String description;
    private String flatNo;
}
