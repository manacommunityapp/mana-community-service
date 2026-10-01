package com.manacommunity.api.cfbos.approval.dto;

import com.manacommunity.api.cfbos.shared.enums.ApprovalState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActionRequest {
    private Long requestId;
    private ApprovalState action;
    private Long actorId;
    private String comments;
}
