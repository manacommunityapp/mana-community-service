package com.manacommunity.api.privacy;

import com.manacommunity.api.marketplace.entity.MarketOrder;
import com.manacommunity.api.marketplace.repository.MarketOrderRepository;
import com.manacommunity.api.privacy.dto.DataRetentionPolicyDto;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionService {

    private final DataRetentionPolicyRepository policyRepo;
    private final VisitorPassRepository visitorPassRepo;
    private final MarketOrderRepository marketOrderRepo;
    private final PrivacyAuditService privacyAuditService;
    private final AuditService auditService;

    private static final int RETENTION_BATCH_SIZE = 2000;
    private static final long RETENTION_BATCH_PAUSE_MS = 50L;

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    @Transactional(readOnly = true)
    public List<DataRetentionPolicyDto> getPolicies(Long communityId) {
        return policyRepo.findByCommunityIdOrCommunityIdIsNull(communityId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public DataRetentionPolicyDto updatePolicy(Long id, DataRetentionPolicyDto dto) {
        DataRetentionPolicy policy = policyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Retention policy not found: " + id));

        if (dto.getRetentionPeriodDays() != null && dto.getRetentionPeriodDays() > 0) {
            policy.setRetentionPeriodDays(dto.getRetentionPeriodDays());
        }
        if (dto.getActionOnExpiry() != null && !dto.getActionOnExpiry().isBlank()) {
            policy.setActionOnExpiry(dto.getActionOnExpiry().toUpperCase());
        }
        if (dto.getIsActive() != null) {
            policy.setIsActive(dto.getIsActive());
        }
        if (dto.getDescription() != null) {
            policy.setDescription(dto.getDescription());
        }

        DataRetentionPolicy saved = policyRepo.save(policy);
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.PRIVACY,
                "DataRetentionPolicy", String.valueOf(saved.getId()));
        return toDto(saved);
    }

    /**
     * Executes data retention enforcement once daily at 2:00 AM.
     * Deliberately non-transactional at the top level: each 2,000-row chunk runs inside
     * its own short-lived REQUIRES_NEW transaction to avoid DB lock contention and replication lag.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeRetentionEnforcement() {
        log.info("Starting automated data retention enforcement job...");
        List<DataRetentionPolicy> activePolicies = policyRepo.findByIsActiveTrue();

        for (DataRetentionPolicy policy : activePolicies) {
            try {
                switch (policy.getDataCategory().toUpperCase()) {
                    case "VISITOR_LOGS"       -> enforceVisitorLogRetention(policy);
                    case "MARKETPLACE_ORDERS" -> enforceMarketplaceOrderRetention(policy);
                    default -> log.debug("No enforcer for retention category: {}", policy.getDataCategory());
                }
            } catch (Exception e) {
                log.error("Error executing retention policy {}: {}", policy.getId(), e.getMessage());
            }
        }
        log.info("Completed automated data retention enforcement.");
    }

    // ── Visitor Logs (Keyset-Chunked & Lock-Free Batches) ───────────────────────

    public int enforceVisitorLogRetention(DataRetentionPolicy policy) {
        int days = policy.getRetentionPeriodDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        boolean isDelete = "DELETE".equalsIgnoreCase(policy.getActionOnExpiry());
        List<VisitorPass.PassStatus> terminalStatuses = List.of(
                VisitorPass.PassStatus.CHECKED_OUT,
                VisitorPass.PassStatus.EXPIRED,
                VisitorPass.PassStatus.REJECTED
        );

        long cursorId = 0L;
        int totalAffected = 0;

        while (true) {
            final long batchCursor = cursorId;
            List<Long> batchIds = visitorPassRepo.findRetentionCandidateIds(
                    policy.getCommunityId(),
                    cutoff,
                    terminalStatuses,
                    batchCursor,
                    isDelete,
                    PageRequest.of(0, RETENTION_BATCH_SIZE)
            );
            if (batchIds == null || batchIds.isEmpty()) break;

            int affectedInBatch = executeBatchTransaction(() ->
                    isDelete
                            ? visitorPassRepo.deletePassBatch(batchIds)
                            : visitorPassRepo.anonymizePassBatch(batchIds));

            totalAffected += affectedInBatch;
            cursorId = batchIds.get(batchIds.size() - 1);

            if (batchIds.size() < RETENTION_BATCH_SIZE) break;
            pauseBetweenBatches();
        }

        if (totalAffected > 0) {
            log.info("Enforced DPDP retention for {} visitor passes older than {} days (action: {})",
                    totalAffected, days, policy.getActionOnExpiry());

            privacyAuditService.record(
                    AuditAction.DELETE_PERSONAL_DATA.name(),
                    "VISITOR_PASS_BATCH",
                    "COUNT:" + totalAffected,
                    policy.getCommunityId(),
                    "Retention policy enforcement: " + policy.getDataCategory()
            );
        }
        return totalAffected;
    }

    private int executeBatchTransaction(java.util.function.IntSupplier action) {
        if (transactionManager == null) {
            return action.getAsInt();
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Integer res = template.execute(status -> action.getAsInt());
        return res != null ? res : 0;
    }

    private void pauseBetweenBatches() {
        try {
            Thread.sleep(RETENTION_BATCH_PAUSE_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Marketplace Orders ─────────────────────────────────────────────────────

    /**
     * Anonymizes delivery address PII from completed/cancelled orders beyond their
     * retention window. Delivery address is the primary PII in order records.
     * The order record itself is kept for financial/audit purposes.
     */
    private void enforceMarketplaceOrderRetention(DataRetentionPolicy policy) {
        int days = policy.getRetentionPeriodDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        // Fetch all completed/cancelled orders and filter by age in-memory
        // (avoids complex JPQL across community boundary; runs at 2 AM)
        List<MarketOrder> oldOrders = marketOrderRepo.findAll(Pageable.unpaged()).stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isBefore(cutoff))
                .filter(o -> o.getStatus() == MarketOrder.OrderStatus.COMPLETED
                          || o.getStatus() == MarketOrder.OrderStatus.CANCELLED
                          || o.getStatus() == MarketOrder.OrderStatus.REFUNDED)
                .filter(o -> o.getDeliveryAddress() != null)  // only rows still containing PII
                .toList();

        if (oldOrders.isEmpty()) return;

        log.info("Enforcing retention for {} marketplace orders older than {} days (action: {})",
                oldOrders.size(), days, policy.getActionOnExpiry());

        for (MarketOrder order : oldOrders) {
            // Always ANONYMIZE for orders — deleting financial records is a compliance risk
            order.setDeliveryAddress(null);
            marketOrderRepo.save(order);
        }

        privacyAuditService.record(
                AuditAction.DELETE_PERSONAL_DATA.name(),
                "MARKETPLACE_ORDER_BATCH",
                "COUNT:" + oldOrders.size(),
                policy.getCommunityId(),
                "Retention policy enforcement: " + policy.getDataCategory()
        );
    }

    // ── DTO mapping ────────────────────────────────────────────────────────────

    private DataRetentionPolicyDto toDto(DataRetentionPolicy p) {
        return DataRetentionPolicyDto.builder()
                .id(p.getId())
                .communityId(p.getCommunityId())
                .dataCategory(p.getDataCategory())
                .retentionPeriodDays(p.getRetentionPeriodDays())
                .actionOnExpiry(p.getActionOnExpiry())
                .isActive(p.getIsActive())
                .description(p.getDescription())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
