package com.manacommunity.api.homeservices.instahelp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstaHelpResponse {

    private Long id;
    private String bookingRef;
    private String serviceType;
    private String subtask;
    private String duration;
    private BigDecimal priceAmount;
    private String notes;
    private String status;
    private String closureOtp;
    private String helperName;
    private Double helperRating;
    private Integer helperEtaMinutes;
    private String helperDistanceLabel;
    private Integer residentRating;
    private String residentFeedback;
    private String bookedAt;
    private String matchedAt;
    private String startedAt;
    private String completedAt;
    private String cancelledAt;
}
