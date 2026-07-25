package com.manacommunity.api.cfbos.accounting.dto;

import com.manacommunity.api.cfbos.accounting.enums.JournalEntryStatus;
import com.manacommunity.api.cfbos.accounting.enums.JournalEntryType;
import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntryDto {
    private Long id;
    private String entryNumber;
    private LocalDate entryDate;
    private JournalEntryType entryType;
    private SourceModule sourceModule;
    private String sourceDocumentType;
    private Long sourceDocumentId;
    private String narration;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private JournalEntryStatus status;
    private LocalDateTime postedAt;
    private List<LineDto> lines;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LineDto {
        private Long id;
        private String accountCode;
        private String accountName;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private String narration;
    }
}
