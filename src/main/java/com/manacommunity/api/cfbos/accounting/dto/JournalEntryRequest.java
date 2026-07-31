package com.manacommunity.api.cfbos.accounting.dto;

import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntryRequest {
    private LocalDate entryDate;
    private String narration;
    private SourceModule sourceModule;
    private String sourceDocumentType;
    private Long sourceDocumentId;
    private List<LineRequest> lines;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LineRequest {
        private String accountCode;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private String narration;
        private Long costCenterId;
        private Long fundId;
    }
}
