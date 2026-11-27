package com.manacommunity.api.onboarding.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionValidationReportDto {

    private Long jobId;
    private Long communityId;
    private int totalRows;
    private int cleanRowCount;
    private int warningRowCount;
    private int fatalErrorRowCount;
    private boolean readyForCommit;
    private BigDecimal openingLedgerSum;
    @Builder.Default
    private Map<String, Long> errorsByColumn = new HashMap<>();
    @Builder.Default
    private List<OnboardingRowRecord> sampleErrors = new ArrayList<>();
}
