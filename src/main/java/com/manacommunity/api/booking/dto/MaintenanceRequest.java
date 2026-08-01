package com.manacommunity.api.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MaintenanceRequest {

    @NotNull
    private Long resourceId;

    @NotBlank
    private String maintenanceType;

    @NotNull
    private String startDate;

    @NotNull
    private String endDate;

    private String title;
    private String description;
    private String status;

    private String reason;
    private Double cost;
    private String vendorName;
    private String vendorContact;
    private String notes;
    private Boolean isRecurring;
    private String recurringPattern;
}
