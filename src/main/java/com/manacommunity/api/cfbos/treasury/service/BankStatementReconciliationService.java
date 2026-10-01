package com.manacommunity.api.cfbos.treasury.service;

import com.manacommunity.api.cfbos.treasury.dto.BankStatementUploadResponse;
import com.manacommunity.api.cfbos.treasury.entity.BankStatementTransaction;
import com.manacommunity.api.cfbos.treasury.enums.ReconciliationStatus;
import com.manacommunity.api.cfbos.treasury.parser.Camt053BankStatementParser;
import com.manacommunity.api.cfbos.treasury.parser.Mt940BankStatementParser;
import com.manacommunity.api.cfbos.treasury.repository.BankStatementTransactionRepository;
import com.manacommunity.api.model.Community;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankStatementReconciliationService {

    private final Mt940BankStatementParser mt940Parser;
    private final Camt053BankStatementParser camt053Parser;
    private final BankStatementTransactionRepository transactionRepository;

    @Transactional
    public BankStatementUploadResponse processStatement(String content, String filename, Community community) {
        List<BankStatementTransaction> parsed;
        if (filename.toLowerCase().endsWith(".xml")) {
            parsed = camt053Parser.parse(content, community);
        } else {
            parsed = mt940Parser.parse(content, community);
        }

        BigDecimal totalCr = BigDecimal.ZERO;
        BigDecimal totalDr = BigDecimal.ZERO;
        int autoMatched = 0;

        for (BankStatementTransaction tx : parsed) {
            if ("CR".equalsIgnoreCase(tx.getEntryType())) {
                totalCr = totalCr.add(tx.getAmount());
            } else {
                totalDr = totalDr.add(tx.getAmount());
            }

            if (tx.getDetectedFlatNumber() != null && !tx.getDetectedFlatNumber().isBlank()) {
                tx.setStatus(ReconciliationStatus.MATCHED_AUTO);
                tx.setConfidenceScore(0.95);
                autoMatched++;
            }
        }

        transactionRepository.saveAll(parsed);

        List<String> notes = new ArrayList<>();
        notes.add("Successfully parsed " + parsed.size() + " statement lines.");
        if (autoMatched > 0) {
            notes.add("Auto-matched " + autoMatched + " transactions via flat/UTR heuristics.");
        }

        return BankStatementUploadResponse.builder()
                .totalParsed(parsed.size())
                .autoMatchedCount(autoMatched)
                .unmatchedCount(parsed.size() - autoMatched)
                .totalCreditAmount(totalCr)
                .totalDebitAmount(totalDr)
                .parsingNotes(notes)
                .build();
    }

    public List<BankStatementTransaction> getUnmatchedTransactions(Long communityId) {
        return transactionRepository.findByCommunityIdAndStatus(communityId, ReconciliationStatus.UNMATCHED);
    }
}
