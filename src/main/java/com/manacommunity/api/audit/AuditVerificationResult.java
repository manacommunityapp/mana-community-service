package com.manacommunity.api.audit;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditVerificationResult {
    private boolean valid;
    private String tenantId;
    private long totalRecordsVerified;
    private Long startSequenceNumber;
    private Long endSequenceNumber;
    private Long tamperedRecordId;
    private Long tamperedSequenceNumber;
    private String failureReason;
    private LocalDateTime verifiedAt;
    private List<String> anomalyDetails;
}
