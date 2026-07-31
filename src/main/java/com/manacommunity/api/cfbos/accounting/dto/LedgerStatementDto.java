package com.manacommunity.api.cfbos.accounting.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LedgerStatementDto {
    private String accountCode;
    private String accountName;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private List<LedgerLine> entries;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LedgerLine {
        private LocalDate date;
        private String entryNumber;
        private String narration;
        private BigDecimal debit;
        private BigDecimal credit;
        private BigDecimal runningBalance;
    }
}
