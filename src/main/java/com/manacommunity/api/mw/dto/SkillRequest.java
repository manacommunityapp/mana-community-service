package com.manacommunity.api.mw.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkillRequest {
    private String skillName;
    private String category;
    private String proficiency;
    private BigDecimal yearsExp;
}
