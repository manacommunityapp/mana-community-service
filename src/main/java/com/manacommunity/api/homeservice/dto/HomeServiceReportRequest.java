package com.manacommunity.api.homeservice.dto;

import com.manacommunity.api.homeservice.model.enums.HomeServiceReportCategory;
import com.manacommunity.api.homeservice.model.enums.HomeServiceReportSeverity;
import lombok.Data;

@Data
public class HomeServiceReportRequest {
    private String communityId;
    private String workerId;
    private String workerName;
    private String reporterUserId;
    private String reporterName;
    private String flatNumber;
    private HomeServiceReportCategory category;
    private HomeServiceReportSeverity severity;
    private String description;
}
