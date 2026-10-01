package com.manacommunity.api.cfbos.penalty.dto;
import com.manacommunity.api.cfbos.penalty.enums.PenaltyStatus;
import com.manacommunity.api.cfbos.penalty.enums.PenaltyType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PenaltyDto {
    private Long id;
    private Long invoiceId;
    private Long residentId;
    private Long penaltyConfigId;
    private String penaltyConfigName;
    private PenaltyType penaltyType;
    private BigDecimal amount;
    private LocalDate calculatedDate;
    private Long appliedToInvoiceId;
    private PenaltyStatus status;
}
