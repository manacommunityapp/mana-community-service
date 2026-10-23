package com.manacommunity.api.homeservices.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DomesticStaffResponse {
    private Long id;
    private String name;
    private String phone;
    private String role;
    private String shiftTime;
    private String workingTowers;
    private BigDecimal monthlySalary;
    private Boolean verified;
    private Boolean policeVerified;
    private Boolean aadhaarOnFile;
    private Double rating;
    private Integer reviewCount;
    private String experience;
    private String status;
    private String photoUrl;
    private LocalDateTime createdAt;
}
