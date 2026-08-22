package com.manacommunity.api.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationReportRowDto {
    private String id;
    private String regCode;
    private String category;
    private String activityTitle;
    private String participantName;
    private String email;
    private String phone;
    private String gotram;
    private Integer devoteeCount;
    private String attendingDevotees;
    private String eventDate;
    private String eventTime;
    private String venue;
    private String mandap;
    private String panditName;
    private Double bookingFee;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionId;
    private String status;
    private Boolean checkedIn;
    private LocalDateTime checkedInAt;
    private String prasadamMode;
    private String notes;
    private LocalDateTime registeredAt;
}