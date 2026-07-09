package com.manacommunity.api.visitor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitorPassResponse {

    private Long id;
    private String passCode;
    private String visitorName;
    private String visitorPhone;
    private String vehicleNumber;
    private String purpose;
    private String passType;
    private String status;
    private String expectedAt;
    private String checkedInAt;
    private String checkedOutAt;
    private String flatNumber;
    private Long residentId;
    private String residentName;
    private Long communityId;
    private String createdAt;
}
