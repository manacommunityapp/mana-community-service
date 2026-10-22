package com.manacommunity.api.healthguard.dto;

import lombok.Data;

@Data
public class BookAmbulanceRequest {
    private String pickupAddress;
    private String destinationAddress;
    private String patientName;
    private String patientPhone;
    private String emergencyDescription;
    private Long incidentId;
}
