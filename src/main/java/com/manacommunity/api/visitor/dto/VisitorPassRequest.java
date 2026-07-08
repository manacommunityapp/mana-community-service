package com.manacommunity.api.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VisitorPassRequest {

    @NotBlank
    private String visitorName;

    private String visitorPhone;
    private String vehicleNumber;
    private String purpose;
    private String passType;
    private String expectedAt;
    private String flatNumber;
}
