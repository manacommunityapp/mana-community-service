package com.manacommunity.api.dto;

import lombok.Data;

@Data
public class CposCrmLeadRequest {
    private String leadName;
    private String email;
    private String phone;
    private String source;
    private String leadType;
    private Integer budgetLakhs;
    private String budgetDisplay;
    private String assignedTo;
}
