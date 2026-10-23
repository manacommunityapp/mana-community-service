package com.manacommunity.api.cfbos.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWorkflowDto {
    private Long id;
    private String name;
    private String entityType;
    private String description;
    private Boolean isActive;
    private List<ApprovalStepDto> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalStepDto {
        private Long id;
        private Integer stepOrder;
        private String approverRole;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private Boolean isAutoApprove;
        private BigDecimal autoApproveBelow;
    }
}
