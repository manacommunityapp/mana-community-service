package com.manacommunity.api.hrm.dto;

import lombok.Data;

@Data
public class CheckOutRequest {
    private Double latitude;
    private Double longitude;
    private String deviceId;
    private String source;
}
