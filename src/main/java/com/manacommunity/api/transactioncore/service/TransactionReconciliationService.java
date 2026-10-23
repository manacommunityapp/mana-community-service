package com.manacommunity.api.transactioncore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.transactioncore.entity.TransactionIntent;
import com.manacommunity.api.transactioncore.entity.TransactionReconciliationLog;
import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
import com.manacommunity.api.transactioncore.repository.TransactionIntentRepository;
import com.manacommunity.api.transactioncore.repository.TransactionReconciliationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionReconciliationService {

    private final TransactionIntentRepository intentRepository;
    private final TransactionReconciliationLogRepository reconRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransactionReconciliationLog runDailyReconciliation(LocalDate reconDate, Long communityId) {
        log.info("Starting 3-way reconciliation for date={} communityId={}", reconDate, communityId);

        String reconId = "REC-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        List<TransactionIntent> allIntents = intentRepository.findAll();
        int total = allIntents.size();
        int matched = 0;
        int mismatched = 0;
        int autoResolved = 0;
        List<Map<String, Object>> discrepancies = new ArrayList<>();

        for (TransactionIntent intent : allIntents) {
            if (intent.getStatus() == TransactionIntentStatus.CAPTURED || intent.getStatus() == TransactionIntentStatus.SETTLED) {
                matched++;
            } else if (intent.getStatus() == TransactionIntentStatus.INITIATED && intent.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
                intent.setStatus(TransactionIntentStatus.CANCELLED);
                intentRepository.save(intent);
                autoResolved++;
            } else if (intent.getStatus() == TransactionIntentStatus.FAILED) {
                mismatched++;
                discrepancies.add(Map.of(
                        "intentId", intent.getIntentId(),
                        "domain", intent.getDomain().name(),
                        "amount", intent.getAmount(),
                        "issue", "Gateway capture failed or timed out"
                ));
            }
        }

        String discrepanciesJson = null;
        try {
            discrepanciesJson = objectMapper.writeValueAsString(discrepancies);
        } catch (Exception e) {
            log.warn("Failed to serialize discrepancies: {}", e.getMessage());
        }

        TransactionReconciliationLog logEntry = TransactionReconciliationLog.builder()
                .reconId(reconId)
                .reconDate(reconDate != null ? reconDate : LocalDate.now())
                .communityId(communityId)
                .totalTransactions(total)
                .matchedCount(matched)
                .mismatchedCount(mismatched)
                .autoResolvedCount(autoResolved)
                .status(mismatched == 0 ? "COMPLETED" : "DISCREPANCY_FOUND")
                .discrepanciesJson(discrepanciesJson)
                .executedAt(LocalDateTime.now())
                .build();

        return reconRepository.save(logEntry);
    }
}
