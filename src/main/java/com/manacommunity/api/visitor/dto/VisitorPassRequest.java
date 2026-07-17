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
    private String passType; // e.g. GUEST, DELIVERY, etc.
    private String expectedAt;
    private String flatNumber;

    // Additional fields for guard walk-in and customizable check-ins
    private Long residentId;
    private String gate;
    private String guard;
    private String visitorPhoto; // Base64 or URL
}
