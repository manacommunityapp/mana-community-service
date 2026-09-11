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
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Iterates all active retention policies and dispatches to the correct enforcer.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
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

    // ── Visitor Logs ───────────────────────────────────────────────────────────

    private void enforceVisitorLogRetention(DataRetentionPolicy policy) {
        int days = policy.getRetentionPeriodDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        List<VisitorPass> oldPasses = visitorPassRepo.findAll().stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isBefore(cutoff))
                .filter(p -> p.getStatus() == VisitorPass.PassStatus.CHECKED_OUT
                          || p.getStatus() == VisitorPass.PassStatus.EXPIRED
                          || p.getStatus() == VisitorPass.PassStatus.REJECTED)
                .toList();

        if (oldPasses.isEmpty()) return;

        log.info("Enforcing retention for {} visitor passes older than {} days (action: {})",
                oldPasses.size(), days, policy.getActionOnExpiry());

        for (VisitorPass pass : oldPasses) {
            if ("DELETE".equalsIgnoreCase(policy.getActionOnExpiry())) {
                visitorPassRepo.delete(pass);
            } else {
                // ANONYMIZE — remove contact & photo, keep pass record for audit
                pass.setVisitorPhone("REDACTED");
                pass.setVisitorPhoto(null);
                pass.setVehicleNumber(null);
                visitorPassRepo.save(pass);
            }
        }

        privacyAuditService.record(
                AuditAction.DELETE_PERSONAL_DATA.name(),
                "VISITOR_PASS_BATCH",
                "COUNT:" + oldPasses.size(),
                policy.getCommunityId(),
                "Retention policy enforcement: " + policy.getDataCategory()
        );
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
