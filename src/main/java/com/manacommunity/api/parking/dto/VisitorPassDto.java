package com.manacommunity.api.parking.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorPassDto {
    private Long id;
    private String passCode;
    private String visitorName;
    private String visitorPhone;
    private String vehicleNumber;
    private String vehicleType;
    private Long spotId;
    private String spotNumber;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String purpose;
    private String status;
}
