package com.manacommunity.api.dto;

import lombok.Data;

@Data
public class CposCrmInboundLeadRequest {
    private String name;
    private String phone;
    private String email;
    private Double budgetLakhs;
    private String propertyType;
    private String portal;       // "99ACRES" | "MAGICBRICKS" | "HOUSING" | "NOBROKER" | "FACEBOOK_AD"
    private String portalLeadId; // external lead ID for dedup
}
