package com.manacommunity.api.cfbos.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitApprovalRequest {
    private String entityType;
    private Long entityId;
    private BigDecimal amount;
    private String remarks;
    private Long submittedBy;
}
