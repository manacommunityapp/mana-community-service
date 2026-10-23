package com.manacommunity.api.cfbos.treasury.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BankStatementUploadResponse {
    private int totalParsed;
    private int autoMatchedCount;
    private int unmatchedCount;
    private BigDecimal totalCreditAmount;
    private BigDecimal totalDebitAmount;
    private List<String> parsingNotes;
}
