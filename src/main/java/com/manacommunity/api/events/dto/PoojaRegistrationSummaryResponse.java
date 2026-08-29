package com.manacommunity.api.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manacommunity.api.events.enums.RegistrationSource;
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
public class PoojaRegistrationSummaryResponse {
    private Long id;
    private String regCode;
    private Long eventId;
    private Long poojaSevaId;
    private Long userId;
    private String participantName;
    private String gotram;
    private String phone;
    private String email;
    private Integer devoteeCount;
    private String attendingDevotees;
    private String poojaSlotName;
    private String poojaSlotDate;
    private String poojaSlotTime;
    private String venue;
    private String category;
    private Double bookingFee;
    private String paymentStatus;
    private String status;
    private Long scheduleId;
    private Long poojaSevaTimeSlotsId;
    private Integer tokenNumber;
    private RegistrationSource registrationSource;
    private Boolean overrideUsed;
    private LocalDateTime createdAt;
}