package com.manacommunity.api.visitor.dto;

import lombok.Data;

@Data
public class WhitelistRequest {
    private String name;
    private String phone;
    private String category;
    private String vehicleNumber;
    private String notes;
}
