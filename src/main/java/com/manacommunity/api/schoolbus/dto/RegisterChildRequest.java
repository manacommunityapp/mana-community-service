package com.manacommunity.api.schoolbus.dto;

import lombok.Data;

@Data
public class RegisterChildRequest {
    private String name;
    private String grade;
    private String schoolName;
    private Long busId;
    private String stopName;
}
