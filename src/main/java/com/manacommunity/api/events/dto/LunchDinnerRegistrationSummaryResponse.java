package com.manacommunity.api.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LunchDinnerRegistrationSummaryResponse {
    private Long id;
    private String regCode;
    private String activityId;
    private String activityTitle;
    private String category;
    private String participantName;
    private String email;
    private String phone;
    private String gotram;
    private String attendingDevotees;
    private Integer devoteeCount;
    private String eventDate;
    private String eventTime;
    private String venue;
    private Double bookingFee;
    private String paymentStatus;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
}