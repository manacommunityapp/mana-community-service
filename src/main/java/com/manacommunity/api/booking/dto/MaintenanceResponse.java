package com.manacommunity.api.booking.dto;

import com.manacommunity.api.booking.entity.enums.MaintenanceStatus;
import com.manacommunity.api.booking.entity.enums.MaintenanceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MaintenanceResponse {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private String startDate;
    private String endDate;
    private String reason;
    private MaintenanceType maintenanceType;
    private MaintenanceStatus status;
    private BigDecimal cost;
    private String vendorName;
    private String vendorContact;
    private String notes;
    private Boolean isRecurring;
    private String createdAt;
}
