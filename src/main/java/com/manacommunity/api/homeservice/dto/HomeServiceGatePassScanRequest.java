package com.manacommunity.api.homeservice.dto;

import com.manacommunity.api.homeservice.model.enums.HomeServiceScanType;
import lombok.Data;

@Data
public class HomeServiceGatePassScanRequest {
    private String gatePassId;
    private String workerId;
    private HomeServiceScanType scanType;
    private String gateName;
    private String guardUserId;
    private String guardName;
}
