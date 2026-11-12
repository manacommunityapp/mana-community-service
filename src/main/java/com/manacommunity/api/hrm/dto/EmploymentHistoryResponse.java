package com.manacommunity.api.hrm.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmploymentHistoryResponse {
    private Long id;
    private String changeType;
    private LocalDate effectiveDate;
    private String previousDesignation;
    private String newDesignation;
    private String previousDepartment;
    private String newDepartment;
    private String previousBranch;
    private String newBranch;
    private Long previousReportingManagerId;
    private Long newReportingManagerId;
    private String reason;
    private String remarks;
    private Long changedBy;
    private LocalDateTime createdAt;
}
