package com.manacommunity.api.hrm.dto;

import lombok.Data;

@Data
public class CheckInRequest {
    private Double latitude;
    private Double longitude;
    private String deviceId;
    private String source;
}
