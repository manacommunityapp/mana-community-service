package com.manacommunity.api.cfbos.accounting.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrialBalanceDto {
    private String fiscalYearName;
    private LocalDate asOfDate;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private List<TrialBalanceLine> lines;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrialBalanceLine {
        private String accountCode;
        private String accountName;
        private String accountType;
        private BigDecimal debitBalance;
        private BigDecimal creditBalance;
    }
}
