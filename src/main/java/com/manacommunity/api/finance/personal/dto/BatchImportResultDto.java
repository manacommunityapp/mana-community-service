package com.manacommunity.api.finance.personal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportResultDto {
    private int importedCount;
    private int failedCount;
    private List<PersonalTransactionDto> importedTransactions;
}
