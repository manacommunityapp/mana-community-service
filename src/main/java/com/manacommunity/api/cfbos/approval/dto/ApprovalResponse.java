package com.manacommunity.api.cfbos.approval.dto;

import com.manacommunity.api.cfbos.shared.enums.ApprovalState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponse {
    private Long id;
    private Long workflowId;
    private String workflowName;
    private String entityType;
    private Long entityId;
    private Integer currentStep;
    private ApprovalState status;
    private Long submittedBy;
    private LocalDateTime submittedAt;
    private BigDecimal amount;
    private String remarks;
    private List<ActionRecord> actions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionRecord {
        private Long id;
        private Integer stepOrder;
        private ApprovalState action;
        private Long actorId;
        private LocalDateTime actedAt;
        private String comments;
    }
}
